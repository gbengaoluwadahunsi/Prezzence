import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useState, useEffect, useRef } from 'react';
import {
  StyleSheet,
  Text,
  View,
  Pressable,
  ScrollView,
  Platform,
  Modal,
  AppState,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { Image } from 'expo-image';
import { useSessionStore } from '../../src/store/sessionStore';
import { useTranslation } from '../../src/hooks/useTranslation';
import { useRecording } from '../../src/hooks/useRecording';
import { useAudioPlayback } from '../../src/hooks/useAudioPlayback';
import { useSettingsStore } from '../../src/store/settingsStore';
import { apiClient } from '../../src/api/client';
import { AvatarPanel } from '../../src/components/interview/AvatarPanel';
import { getDuixModelNameForPersona, isDuixAvatarAvailable } from '../../src/native/DuixAvatarView';
import { getLocalPersonaImage, getLocalizedPersonaCopy } from '../../src/utils/personaAssets';
import {
  PresenceCameraView,
  isPresenceCameraAvailable,
  requestPresenceCameraPermission,
  type PresenceMetrics,
} from '../../src/native/PresenceCameraView';
import { synthesizeSpeech } from '../../src/services/voiceProvider';
import {
  isLocalWhisperEnabled,
  isLocalWhisperPrepared,
  prepareLocalWhisper,
  startLocalWhisperCapture,
  transcribeAnswerLocally,
  type LocalWhisperCapture,
} from '../../src/services/localWhisper';
import { trackEvent } from '../../src/services/analytics';
import { getInterviewerReaction } from '../../src/utils/interviewerPsychology';
import { BrandWordmark } from '../../src/components/BrandWordmark';
import { getPersonasForInterview } from '../../src/data/personas';
import type { Persona } from '../../src/types';
import type { SpeechSynthesisResult } from '../../src/services/voiceProvider';

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  textSecondary: '#8A8A9A',
  card: '#1C1C2E',
  success: '#00D68F',
  danger: '#FF4757',
};

const LOCAL_TRANSCRIPTION_TIMEOUT_MS = 7000;

type InterviewPhase = 'intro' | 'speaking' | 'answering' | 'processing' | 'finished';

type PresenceSummary = {
  faceVisibility: number;
  eyeContact: number;
  headStability: number;
  posture: number;
  expressionEnergy: number;
  sampleCount: number;
};

type DuixSpeechReady = {
  source: string;
  durationMs?: number;
};

const DUIX_SPEECH_READY_TIMEOUT_MS = 6000;

function estimateSpeechDurationMs(text: string) {
  const wordCount = text.trim().split(/\s+/).filter(Boolean).length;
  return Math.max(2200, Math.round((wordCount / 145) * 60_000) + 700);
}

function wait(ms: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, ms));
}

function audioFilenameForMimeType(mimeType?: string) {
  const normalized = (mimeType || '').toLowerCase();
  if (normalized.includes('wav')) return 'answer.wav';
  if (normalized.includes('mpeg') || normalized.includes('mp3')) return 'answer.mp3';
  if (normalized.includes('webm')) return 'answer.webm';
  if (normalized.includes('ogg')) return 'answer.ogg';
  return 'answer.m4a';
}

async function readAudioAsBase64(uri: string) {
  // @ts-ignore - expo-file-system legacy API is still available in this app runtime.
  const FileSystem = require('expo-file-system/legacy');
  return FileSystem.readAsStringAsync(uri, {
    encoding: (FileSystem as any).EncodingType?.Base64 || 'base64',
  });
}

type InterviewRoomBoundaryState = {
  hasError: boolean;
  message?: string;
};

class InterviewRoomBoundary extends React.Component<React.PropsWithChildren, InterviewRoomBoundaryState> {
  state: InterviewRoomBoundaryState = { hasError: false };

  static getDerivedStateFromError(error: Error): InterviewRoomBoundaryState {
    return { hasError: true, message: error.message };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('[InterviewRoomBoundary] Recovered from render error:', error, info.componentStack);
  }

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <SafeAreaView style={styles.container}>
        <LinearGradient
          colors={['rgba(108, 99, 255, 0.08)', 'transparent']}
          style={StyleSheet.absoluteFill}
        />
        <View style={styles.recoveryScreen}>
          <BrandWordmark size={38} textSize={16} />
          <View style={styles.recoveryIcon}>
            <Feather name="alert-triangle" size={30} color="#FFB020" />
          </View>
          <Text style={styles.recoveryTitle}>Interview room needs a reset</Text>
          <Text style={styles.recoveryBody}>
            The session is still saved. Return to Home, then continue the unfinished interview or start again.
          </Text>
          {this.state.message ? (
            <Text style={styles.recoveryMeta} numberOfLines={2}>{this.state.message}</Text>
          ) : null}
          <Pressable
            onPress={() => router.replace('/(tabs)/home' as any)}
            style={styles.recoveryButton}
          >
            <Text style={styles.recoveryButtonText}>Return to Home</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }
}

export default function InterviewRoom() {
  return (
    <InterviewRoomBoundary>
      <InterviewRoomContent />
    </InterviewRoomBoundary>
  );
}

function InterviewRoomContent() {
  const {
    questions,
    activeSessionId,
    selectedPersonas,
    interviewerMode,
    language,
    addAnswer,
    currentQuestionIndex,
    setCurrentQuestionIndex,
    deviceCapabilities,
    resetSession,
  } = useSessionStore();
  const { cameraPresenceCoachEnabled } = useSettingsStore();
  const { t } = useTranslation();
  const sessionDeviceCapabilities = deviceCapabilities?.sessionId === activeSessionId ? deviceCapabilities : null;
  const effectiveInterviewerMode = interviewerMode === 'peer' ? 'single' : interviewerMode;
  const presenceEnabledForSession = sessionDeviceCapabilities
    ? sessionDeviceCapabilities.presenceCoach === 'enabled'
    : Platform.OS === 'android' && cameraPresenceCoachEnabled && isPresenceCameraAvailable();
  const [phase, setPhase] = useState<InterviewPhase>('speaking');
  const { isRecording, isPaused: isRecordingPaused, duration, startRecording, pauseRecording, resumeRecording, stopRecording } = useRecording();
  const {
    playAudio,
    pauseAudio,
    resumeAudio,
    stopAudio,
  } = useAudioPlayback();
  const [lastFeedback, setLastFeedback] = useState<any>(null);
  const [retryBaseline, setRetryBaseline] = useState<{ questionNumber: number; score: number; transcript: string } | null>(null);
  const [presentedQuestionText, setPresentedQuestionText] = useState('');
  const [questionAudioActive, setQuestionAudioActive] = useState(false);
  const [duixSpeechSource, setDuixSpeechSource] = useState<string | undefined>(undefined);
  const [readyDuixModelName, setReadyDuixModelName] = useState<string | null>(null);
  const [turnCueActive, setTurnCueActive] = useState(false);
  const [interviewerTurnReady, setInterviewerTurnReady] = useState(false);
  const [processingStage, setProcessingStage] = useState('');
  const [isInterviewerPaused, setIsInterviewerPaused] = useState(false);
  const [isStartingAnswer, setIsStartingAnswer] = useState(false);
  const [isFinishingAnswer, setIsFinishingAnswer] = useState(false);
  const [presenceSnapshot, setPresenceSnapshot] = useState<PresenceMetrics | null>(null);
  const [presenceCameraBlocked, setPresenceCameraBlocked] = useState(false);
  const [presenceCameraError, setPresenceCameraError] = useState<string | null>(null);
  const [presenceCameraFrameActive, setPresenceCameraFrameActive] = useState(false);
  const [presenceCoachTimedOut, setPresenceCoachTimedOut] = useState(false);
  const [presencePermissionPending, setPresencePermissionPending] = useState(false);
  const [presenceCameraKey, setPresenceCameraKey] = useState(0);
  const [presenceEnabledForAnswer, setPresenceEnabledForAnswer] = useState(false);
  const [localWhisperReady, setLocalWhisperReady] = useState(() => isLocalWhisperPrepared(language));
  const [interviewerReaction, setInterviewerReaction] = useState(() => getInterviewerReaction({
    phase: 'speaking',
    personality: 'neutral',
  }));
  const speechRunRef = useRef(0);
  const currentSpeechRef = useRef<SpeechSynthesisResult | undefined>(undefined);
  const finishingAnswerRef = useRef(false);
  const readyDuixSpeechSourceRef = useRef<Map<string, DuixSpeechReady>>(new Map());
  const duixSpeechWaitersRef = useRef<Map<string, Array<(ready: DuixSpeechReady | null) => void>>>(new Map());
  const duixSpeechEndWaitersRef = useRef<Map<string, Array<() => void>>>(new Map());
  const presenceSamplesRef = useRef<PresenceMetrics[]>([]);
  const presenceCameraFrameActiveRef = useRef(false);
  const presenceAutoRestartedRef = useRef(false);
  const localWhisperCaptureRef = useRef<LocalWhisperCapture | null>(null);
  const localWhisperCapturePromiseRef = useRef<Promise<LocalWhisperCapture | null> | null>(null);

  const visiblePersonas = React.useMemo(() => {
    const saved = selectedPersonas.filter(Boolean);
    return saved.length ? saved : getPersonasForInterview(effectiveInterviewerMode, 'neutral');
  }, [effectiveInterviewerMode, selectedPersonas]);
  const currentQuestion = questions[currentQuestionIndex];
  const currentPersona = resolveQuestionPersona(
    visiblePersonas,
    currentQuestion?.interviewer_name,
    effectiveInterviewerMode === 'panel' ? currentQuestionIndex : 0,
  );
  const activeStagePersonas = visiblePersonas;
  const currentQuestionText = cleanQuestionText(
    currentQuestion?.text || '',
    visiblePersonas,
    currentQuestion?.interviewer_name
  );
  const activePersonaCopy = currentPersona ? getLocalizedPersonaCopy(currentPersona, language) : null;
  const activeDuixModelName = React.useMemo(
    () => getDuixModelNameForPersona(currentPersona),
    [currentPersona?.id, currentPersona?.name, currentPersona?.personality],
  );
  const shouldUseDuixForSession = sessionDeviceCapabilities?.avatar !== 'static';
  const shouldWaitForDuix = Platform.OS === 'android' && shouldUseDuixForSession && isDuixAvatarAvailable();
  const shouldUseLocalWhisper = sessionDeviceCapabilities?.transcription === 'local-whisper' || localWhisperReady;
  const presenceCoachEnabledForStage = phase === 'answering'
    ? presenceEnabledForAnswer
    : presenceEnabledForSession;
  const shouldRunPresenceCoach = Platform.OS === 'android'
    && presenceCoachEnabledForStage
    && !presenceCameraBlocked
    && isPresenceCameraAvailable();
  const shouldShowPresenceCoach = Platform.OS === 'android'
    && presenceCoachEnabledForStage
    && isPresenceCameraAvailable();
  const presenceCameraNeedsRetry = Boolean(
    presenceCameraError && !presenceSnapshot && !presenceCameraFrameActive && !presencePermissionPending
  );
  const shouldRenderPresenceCoach = shouldShowPresenceCoach
    && phase === 'answering'
    && !presencePermissionPending
    && !presenceCameraBlocked
    && !presenceCameraError
    && !presenceCoachTimedOut
    && !presenceCameraNeedsRetry;
  const canBeginAnswer = interviewerTurnReady;

  useEffect(() => {
    if (!activeSessionId || questions.length === 0) {
      router.replace('/(tabs)/home' as any);
    }
  }, [activeSessionId, questions.length]);

  useEffect(() => {
    if (!activeSessionId || !isLocalWhisperEnabled(language)) {
      setLocalWhisperReady(false);
      return;
    }

    if (isLocalWhisperPrepared(language)) {
      setLocalWhisperReady(true);
      return;
    }

    let cancelled = false;
    prepareLocalWhisper({
      language,
      onStatus: __DEV__ ? (status) => {
        if (!cancelled) console.log('[LocalWhisper:prepare]', status);
      } : undefined,
    }).then((ready) => {
      if (!cancelled) setLocalWhisperReady(ready);
    }).catch((error) => {
      if (__DEV__ && !cancelled) {
        console.warn('[LocalWhisper:prepare] Failed:', error?.message || error);
      }
      if (!cancelled) setLocalWhisperReady(false);
    });

    return () => {
      cancelled = true;
    };
  }, [activeSessionId, language]);

  const markDuixSpeechReady = React.useCallback((source: string, durationMs?: number) => {
    const ready = { source, durationMs };
    readyDuixSpeechSourceRef.current.set(source, ready);
    const waiters = duixSpeechWaitersRef.current.get(source) || [];
    duixSpeechWaitersRef.current.delete(source);
    waiters.forEach((resolve) => resolve(ready));
  }, []);

  const markDuixSpeechFailed = React.useCallback((source: string) => {
    readyDuixSpeechSourceRef.current.delete(source);
    const waiters = duixSpeechWaitersRef.current.get(source) || [];
    duixSpeechWaitersRef.current.delete(source);
    waiters.forEach((resolve) => resolve(null));
    const endWaiters = duixSpeechEndWaitersRef.current.get(source) || [];
    duixSpeechEndWaitersRef.current.delete(source);
    endWaiters.forEach((resolve) => resolve());
  }, []);

  const markDuixSpeechEnded = React.useCallback((source: string) => {
    const waiters = duixSpeechEndWaitersRef.current.get(source) || [];
    duixSpeechEndWaitersRef.current.delete(source);
    waiters.forEach((resolve) => resolve());
  }, []);

  const waitForDuixSpeechReady = React.useCallback((source: string, timeoutMs = DUIX_SPEECH_READY_TIMEOUT_MS) => {
    const ready = readyDuixSpeechSourceRef.current.get(source);
    if (!shouldWaitForDuix || ready) {
      return Promise.resolve(ready || null);
    }

    return new Promise<DuixSpeechReady | null>((resolve) => {
      let settled = false;
      let timeout: ReturnType<typeof setTimeout>;

      const finish = (readySpeech: DuixSpeechReady | null) => {
        if (settled) return;
        settled = true;
        clearTimeout(timeout);
        const waiters = duixSpeechWaitersRef.current.get(source);
        if (waiters) {
          duixSpeechWaitersRef.current.set(source, waiters.filter((waiter) => waiter !== finish));
        }
        resolve(readySpeech);
      };

      const waiters = duixSpeechWaitersRef.current.get(source) || [];
      waiters.push(finish);
      duixSpeechWaitersRef.current.set(source, waiters);
      timeout = setTimeout(() => finish(null), timeoutMs);
    });
  }, [shouldWaitForDuix]);

  const waitForDuixSpeechEnd = React.useCallback((source: string, timeoutMs: number) => {
    if (!shouldWaitForDuix) {
      return Promise.resolve();
    }

    return new Promise<void>((resolve) => {
      let settled = false;
      let timeout: ReturnType<typeof setTimeout>;

      const finish = () => {
        if (settled) return;
        settled = true;
        clearTimeout(timeout);
        const waiters = duixSpeechEndWaitersRef.current.get(source);
        if (waiters) {
          duixSpeechEndWaitersRef.current.set(source, waiters.filter((waiter) => waiter !== finish));
        }
        resolve();
      };

      const waiters = duixSpeechEndWaitersRef.current.get(source) || [];
      waiters.push(finish);
      duixSpeechEndWaitersRef.current.set(source, waiters);
      timeout = setTimeout(finish, timeoutMs);
    });
  }, [shouldWaitForDuix]);

  useEffect(() => {
    if (questions.length > 0 && currentQuestionIndex >= questions.length) {
      setCurrentQuestionIndex(Math.max(0, questions.length - 1));
    }
  }, [currentQuestionIndex, questions.length, setCurrentQuestionIndex]);

  useEffect(() => {
    if (!lastFeedback) {
      setInterviewerReaction(getInterviewerReaction({
        phase,
        personality: currentPersona?.personality,
      }));
    }
    // When entering speaking phase, play TTS
    if (phase === 'speaking' && currentQuestion) {
      playQuestionAudio(currentQuestionText);
    }

    return () => {
      speechRunRef.current += 1;
      stopAudio();
      setQuestionAudioActive(false);
      setDuixSpeechSource(undefined);
      readyDuixSpeechSourceRef.current.clear();
      duixSpeechEndWaitersRef.current.forEach((waiters) => waiters.forEach((resolve) => resolve()));
      duixSpeechEndWaitersRef.current.clear();
      setTurnCueActive(false);
      setInterviewerTurnReady(false);
    };
  }, [
    currentQuestionIndex,
    phase,
    currentQuestionText,
    currentPersona?.personality,
    activeDuixModelName,
    shouldWaitForDuix,
  ]);

  useEffect(() => {
    setReadyDuixModelName((readyModelName) => (
      readyModelName === activeDuixModelName ? readyModelName : null
    ));
  }, [activeDuixModelName]);

  useEffect(() => {
    setPresentedQuestionText('');
    setQuestionAudioActive(false);
    setDuixSpeechSource(undefined);
    readyDuixSpeechSourceRef.current.clear();
    duixSpeechEndWaitersRef.current.forEach((waiters) => waiters.forEach((resolve) => resolve()));
    duixSpeechEndWaitersRef.current.clear();
    setTurnCueActive(false);
    setInterviewerTurnReady(false);
    setIsInterviewerPaused(false);
  }, [currentQuestionIndex, currentQuestionText]);

  useEffect(() => {
    if (phase !== 'answering' || !shouldShowPresenceCoach) return;

    presenceAutoRestartedRef.current = false;
    presenceCameraFrameActiveRef.current = false;
    setPresenceCameraFrameActive(false);
    setPresenceCameraKey((key) => key + 1);
    setPresenceCameraError(null);
    setPresenceCoachTimedOut(false);

    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        setPresenceCameraError(null);
        setPresenceCoachTimedOut(false);
        setPresenceCameraKey((key) => key + 1);
      }
    });

    return () => {
      subscription.remove();
    };
  }, [phase, currentQuestionIndex, shouldShowPresenceCoach]);

  useEffect(() => {
    if (
      phase !== 'answering'
      || !shouldRunPresenceCoach
      || presenceCameraBlocked
      || presencePermissionPending
      || presenceSnapshot
      || presenceAutoRestartedRef.current
    ) {
      return;
    }

    const restartTimeout = setTimeout(() => {
      presenceAutoRestartedRef.current = true;
      setPresenceCameraError(null);
      setPresenceCameraKey((key) => key + 1);
    }, 2200);

    const failTimeout = setTimeout(() => {
      if (presenceSamplesRef.current.length === 0) {
        setPresenceCoachTimedOut(true);
        setPresenceCameraError('Camera coach was skipped for this answer.');
      }
    }, 5200);

    return () => {
      clearTimeout(restartTimeout);
      clearTimeout(failTimeout);
    };
  }, [
    phase,
    shouldRunPresenceCoach,
    presenceCameraBlocked,
    presencePermissionPending,
    presenceSnapshot,
    currentQuestionIndex,
  ]);


  const playQuestionAudio = async (text: string) => {
    const speechRun = speechRunRef.current + 1;
    speechRunRef.current = speechRun;
    setInterviewerTurnReady(false);

    try {
      const speech = await synthesizeSpeech({
        text,
        personality: currentPersona?.personality || 'neutral',
        lang: language || 'en',
      });
      currentSpeechRef.current = speech;

      if (speechRunRef.current !== speechRun || phase !== 'speaking') return;
      if (__DEV__) {
        console.log('[InterviewRoom] Playing TTS:', speech.provider, speech.engine || 'default');
      }
      trackEvent('question_presented', {
        questionNumber: currentQuestion?.number,
        provider: speech.provider,
        engine: speech.engine,
      });
      if (speech.speak) {
        try {
          setQuestionAudioActive(true);
          setInterviewerTurnReady(false);
          setIsInterviewerPaused(false);
          await speech.speak();
        } catch (error) {
          console.warn('[InterviewRoom] Direct speech failed, using backend fallback:', error);
          const fallbackSpeech = await synthesizeSpeech({
            text,
            personality: currentPersona?.personality || 'neutral',
            lang: language || 'en',
            preferBackend: true,
          });
          currentSpeechRef.current = fallbackSpeech;
          if (speechRunRef.current !== speechRun || phase !== 'speaking') return;
          if (!fallbackSpeech.audioUrl) {
            throw new Error('Backend speech fallback did not return audio.');
          }
          setDuixSpeechSource(fallbackSpeech.audioUrl);
          const duixReady = await waitForDuixSpeechReady(fallbackSpeech.audioUrl);
          if (speechRunRef.current !== speechRun || phase !== 'speaking') return;
          const maxSpeechWaitMs = Math.max(2500, duixReady?.durationMs || estimateSpeechDurationMs(text)) + 2500;
          const speechEndPromise = shouldWaitForDuix && duixReady
            ? waitForDuixSpeechEnd(fallbackSpeech.audioUrl, maxSpeechWaitMs)
            : null;
          setQuestionAudioActive(true);
          setInterviewerTurnReady(false);
          setIsInterviewerPaused(false);
          if (shouldWaitForDuix && duixReady) {
            await speechEndPromise;
          } else {
            setDuixSpeechSource(undefined);
            await playAudio(fallbackSpeech.audioUrl, { downloadFirst: shouldWaitForDuix });
          }
        }
      } else if (speech.audioUrl) {
        setDuixSpeechSource(speech.audioUrl);
        const duixReady = await waitForDuixSpeechReady(speech.audioUrl);
        if (speechRunRef.current !== speechRun || phase !== 'speaking') return;
        const maxSpeechWaitMs = Math.max(2500, duixReady?.durationMs || estimateSpeechDurationMs(text)) + 2500;
        const speechEndPromise = shouldWaitForDuix && duixReady
          ? waitForDuixSpeechEnd(speech.audioUrl, maxSpeechWaitMs)
          : null;
        setQuestionAudioActive(true);
        setInterviewerTurnReady(false);
        setIsInterviewerPaused(false);
        if (shouldWaitForDuix && duixReady) {
          await speechEndPromise;
        } else {
          setDuixSpeechSource(undefined);
          await playAudio(speech.audioUrl, { downloadFirst: shouldWaitForDuix });
        }
      } else {
        throw new Error('No playable speech output was returned.');
      }
      if (speechRunRef.current !== speechRun) return;
      setQuestionAudioActive(false);
      setDuixSpeechSource(undefined);
      readyDuixSpeechSourceRef.current.clear();
      setIsInterviewerPaused(false);
      setInterviewerTurnReady(true);
      showStartSpeakingCue(speechRun);
      if (__DEV__) {
        console.log('[InterviewRoom] TTS playback complete');
      }
    } catch (err) {
      if (speechRunRef.current !== speechRun) return;
      setQuestionAudioActive(false);
      setDuixSpeechSource(undefined);
      readyDuixSpeechSourceRef.current.clear();
      setTurnCueActive(false);
      setIsInterviewerPaused(false);
      setInterviewerTurnReady(true);
      console.error('[InterviewRoom] Failed to play TTS:', err);
      // If critical network error, show connection screen
      if (!text) router.push('/errors/network-error' as any);
    }
  };

  const showStartSpeakingCue = (speechRun: number) => {
    if (speechRunRef.current !== speechRun || phase !== 'speaking') return;

    setTurnCueActive(true);
    setTimeout(() => {
      if (speechRunRef.current === speechRun) {
        setTurnCueActive(false);
      }
    }, 1800);
  };

  const playCoachingAudio = async (text?: string) => {
    if (!text) return;
    const speechRun = speechRunRef.current + 1;
    speechRunRef.current = speechRun;

    try {
      const speech = await synthesizeSpeech({
        text: `A stronger answer would be: ${text}`,
        personality: currentPersona?.personality || 'neutral',
        lang: language || 'en',
        preferBackend: true,
      });
      if (speechRunRef.current !== speechRun) return;
      if (speech.audioUrl) {
        await playAudio(speech.audioUrl);
      } else if (speech.speak) {
        await speech.speak();
      }
    } catch (error) {
      console.warn('[InterviewRoom] Failed to play coaching TTS:', error);
    }
  };

  const handleAnswerBegin = async () => {
    if (isStartingAnswer || isFinishingAnswer) return;
    if (!canBeginAnswer) return;
    setIsStartingAnswer(true);
    presenceSamplesRef.current = [];
    presenceCameraFrameActiveRef.current = false;
    setPresenceSnapshot(null);
    setPresenceCameraFrameActive(false);
    setPresenceCameraError(null);
    setPresenceCameraBlocked(false);
    setPresenceCameraKey((key) => key + 1);
    let shouldEnablePresenceForThisAnswer = presenceEnabledForSession && isPresenceCameraAvailable();
    setPresenceEnabledForAnswer(shouldEnablePresenceForThisAnswer);
    if (shouldEnablePresenceForThisAnswer) {
      setPresencePermissionPending(true);
      const granted = await requestPresenceCameraPermission().catch(() => false);
      setPresencePermissionPending(false);
      shouldEnablePresenceForThisAnswer = granted;
      setPresenceEnabledForAnswer(granted);
      if (!granted) {
        setPresenceCameraBlocked(true);
        setPresenceCameraError('Camera permission is required for presence coaching.');
      }
    }
    speechRunRef.current += 1;
    stopAudio(); // Stop TTS if still playing
    setQuestionAudioActive(false);
    setDuixSpeechSource(undefined);
    readyDuixSpeechSourceRef.current.clear();
    duixSpeechEndWaitersRef.current.forEach((waiters) => waiters.forEach((resolve) => resolve()));
    duixSpeechEndWaitersRef.current.clear();
    setTurnCueActive(false);
    setInterviewerTurnReady(false);
    currentSpeechRef.current = undefined;
    setIsInterviewerPaused(false);
    setInterviewerReaction(getInterviewerReaction({
      phase: 'answering',
      personality: currentPersona?.personality,
    }));
    setPhase('answering');
    localWhisperCaptureRef.current = null;
    localWhisperCapturePromiseRef.current = null;
    const started = await startRecording();
    setIsStartingAnswer(false);
    if (!started) {
      localWhisperCaptureRef.current = null;
      localWhisperCapturePromiseRef.current = null;
      setPhase('speaking');
      router.push('/errors/microphone-denied' as any);
      return;
    }

    if (shouldUseLocalWhisper) {
      localWhisperCapturePromiseRef.current = startLocalWhisperCapture({
        language,
        onStatus: __DEV__ ? (status) => console.log('[LocalWhisper]', status) : undefined,
      });
      localWhisperCapturePromiseRef.current.then((localCapture) => {
        localWhisperCaptureRef.current = localCapture;
      }).catch((error) => {
        if (__DEV__) {
          console.warn('[LocalWhisper] Capture start skipped:', error?.message || error);
        }
      });
    }
  };

  const handlePresencePermissionRetry = async () => {
    setPresencePermissionPending(true);
    setPresenceCameraError(null);
    let granted = false;
    try {
      granted = await requestPresenceCameraPermission();
    } finally {
      setPresencePermissionPending(false);
    }
    setPresenceCameraBlocked(!granted);
    if (!granted) {
      setPresenceCameraError('Camera permission is still blocked. Enable it in Android app settings to use presence coaching.');
    } else {
      presenceCameraFrameActiveRef.current = false;
      setPresenceCameraFrameActive(false);
      setPresenceCameraError(null);
      setPresenceCameraKey((key) => key + 1);
    }
  };

  const handlePauseInterviewer = () => {
    const paused = pauseAudio();
    if (!paused) {
      stopAudio();
      setDuixSpeechSource(undefined);
      readyDuixSpeechSourceRef.current.clear();
      setInterviewerTurnReady(false);
    }
    setQuestionAudioActive(false);
    setTurnCueActive(false);
    setIsInterviewerPaused(true);
  };

  const handleResumeInterviewer = () => {
    const resumed = resumeAudio();
    setIsInterviewerPaused(false);
    if (resumed) {
      setQuestionAudioActive(true);
      setInterviewerTurnReady(false);
      return;
    }
    if (currentQuestionText) {
      playQuestionAudio(presentedQuestionText || currentQuestionText);
    }
  };

  const handleRepeatQuestion = () => {
    if (!currentQuestionText || isStartingAnswer || isFinishingAnswer) return;
    speechRunRef.current += 1;
    stopAudio();
    setPresentedQuestionText(currentQuestionText);
    setQuestionAudioActive(false);
    setDuixSpeechSource(undefined);
    readyDuixSpeechSourceRef.current.clear();
    setTurnCueActive(false);
    setInterviewerTurnReady(false);
    setIsInterviewerPaused(false);
    playQuestionAudio(currentQuestionText);
  };

  const handleClarifyQuestion = () => {
    if (!currentQuestionText || isStartingAnswer || isFinishingAnswer) return;
    const clarifiedQuestion = buildClarifiedQuestion(currentQuestionText);
    speechRunRef.current += 1;
    stopAudio();
    setPresentedQuestionText(clarifiedQuestion);
    setQuestionAudioActive(false);
    setDuixSpeechSource(undefined);
    readyDuixSpeechSourceRef.current.clear();
    setTurnCueActive(false);
    setInterviewerTurnReady(false);
    setIsInterviewerPaused(false);
    playQuestionAudio(clarifiedQuestion);
  };

  const handlePauseAnswer = async () => {
    localWhisperCaptureRef.current?.pause();
    await pauseRecording();
  };

  const handleResumeAnswer = async () => {
    localWhisperCaptureRef.current?.resume();
    await resumeRecording();
  };

  const advanceQuestion = async () => {
    setLastFeedback(null);
    setProcessingStage('');
    setInterviewerReaction(getInterviewerReaction({
      phase: 'speaking',
      personality: currentPersona?.personality,
    }));
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
      setPhase('speaking');
    } else {
      const completedSessionId = activeSessionId;
      try {
        if (completedSessionId) {
          await apiClient.patch(`/api/sessions/${completedSessionId}/complete`);
        }
      } catch (err) {
        console.error('[InterviewRoom] Failed to complete session:', err);
      }
      setPhase('finished');
      trackEvent('session_completed', {
        questionCount: questions.length,
      });
      router.replace('/(tabs)/progress' as any);
      setTimeout(() => {
        resetSession();
      }, 0);
    }
  };

  const retryCurrentQuestion = () => {
    if (lastFeedback && currentQuestion) {
      setRetryBaseline({
        questionNumber: currentQuestion.number,
        score: lastFeedback.score || 0,
        transcript: lastFeedback.transcript || '',
      });
    }
    setLastFeedback(null);
    setProcessingStage('');
    setInterviewerReaction(getInterviewerReaction({
      phase: 'speaking',
      personality: currentPersona?.personality,
    }));
    trackEvent('question_retry_started', {
      questionNumber: currentQuestion?.number,
    });
    setPhase('speaking');
  };

  const handleAnswerFinish = async () => {
    if (finishingAnswerRef.current || isStartingAnswer) return;
    if (!currentQuestion) {
      router.replace('/(tabs)/home' as any);
      return;
    }

    finishingAnswerRef.current = true;
    setIsFinishingAnswer(true);
    setPhase('processing');
    setProcessingStage('Transcribing');
    const processingStartedAt = Date.now();
    let recordingStoppedAt = processingStartedAt;
    let analysisStartedAt = processingStartedAt;
    let processingTimers: ReturnType<typeof setTimeout>[] = [];
    const clearProcessingTimers = () => {
      processingTimers.forEach(clearTimeout);
      processingTimers = [];
    };
    const scheduleProcessingStage = (delayMs: number, message: string) => {
      processingTimers.push(setTimeout(() => setProcessingStage(message), delayMs));
    };
    let localCapture = localWhisperCaptureRef.current;
    if (!localCapture && localWhisperCapturePromiseRef.current) {
      localCapture = await Promise.race([
        localWhisperCapturePromiseRef.current.catch(() => null),
        new Promise<null>((resolve) => setTimeout(() => resolve(null), 350)),
      ]);
    }
    localWhisperCaptureRef.current = null;
    localWhisperCapturePromiseRef.current = null;
    const audio = await stopRecording({ includeBase64: false });
    recordingStoppedAt = Date.now();
    const presenceMetrics = summarizePresenceSamples(presenceSamplesRef.current);
    const presenceAttempted = shouldRunPresenceCoach || presenceSamplesRef.current.length > 0;
    setPresenceEnabledForAnswer(false);
    setProcessingStage(audio ? 'Transcribing' : 'Checking your recording...');
    if (audio) {
      scheduleProcessingStage(3500, 'Transcribing');
      scheduleProcessingStage(8000, 'Transcribing');
    }

    let feedback = null;

    if (!audio) {
      await localCapture?.stop();
      setLastFeedback({
        score: 0,
        feedback: 'No clear recording was captured. Try the question again and speak for at least a few seconds.',
        transcript: '',
        presenceMetrics,
        presenceAttempted,
      });
      setInterviewerReaction(getInterviewerReaction({
        phase: 'processing',
        score: 0,
        feedback: 'No clear recording was captured.',
        personality: currentPersona?.personality,
      }));
      finishingAnswerRef.current = false;
      setIsFinishingAnswer(false);
      clearProcessingTimers();
      setProcessingStage('');
      return;
    }

    if (audio && activeSessionId) {
      try {
        analysisStartedAt = Date.now();
        let response;
        let localTranscript = '';
        let localTranscriptSource = '';
        let localTranscriptionMs = 0;
        if (Platform.OS === 'web') {
          const base64 = audio.base64 || await readAudioAsBase64(audio.uri);
          response = await apiClient.post(`/api/sessions/${activeSessionId}/answers`, {
            question_id: currentQuestion.number,
            audio_base64: base64,
            question_text: currentQuestionText,
            audio_mime_type: audio.mimeType,
            audio_duration_seconds: audio.durationSeconds,
            audio_base64_chars: base64.length,
          }, { timeoutMs: 45000 });
        } else {
          if (shouldUseLocalWhisper && isLocalWhisperEnabled(language)) {
            const localStartedAt = Date.now();
            const localStatus = (status: string) => {
              clearProcessingTimers();
              setProcessingStage(status);
            };
            const localResult = localCapture
              ? await localCapture.stopAndTranscribe({
                language,
                timeoutMs: LOCAL_TRANSCRIPTION_TIMEOUT_MS,
                onStatus: localStatus,
              })
              : await transcribeAnswerLocally(audio.uri, {
                language,
                audioMimeType: audio.mimeType,
                timeoutMs: LOCAL_TRANSCRIPTION_TIMEOUT_MS,
                onStatus: localStatus,
              });
            localTranscriptionMs = Date.now() - localStartedAt;
            if (localResult?.transcript) {
              localTranscript = localResult.transcript;
              localTranscriptSource = localResult.source;
              setProcessingStage('Transcribing');
            }
          }

          try {
            if (localTranscript) {
              response = await apiClient.post(`/api/sessions/${activeSessionId}/answers`, {
                question_id: currentQuestion.number,
                question_text: currentQuestionText,
                transcript: localTranscript,
                transcript_source: localTranscriptSource,
                audio_mime_type: audio.mimeType,
                audio_duration_seconds: audio.durationSeconds,
                audio_base64_chars: 0,
              }, { timeoutMs: 30000 });
            } else {
              const formData = new FormData();
              formData.append('question_id', String(currentQuestion.number));
              formData.append('question_text', currentQuestionText);
              formData.append('audio_duration_seconds', String(audio.durationSeconds || 0));
              formData.append('audio', {
                uri: audio.uri,
                type: audio.mimeType,
                name: audioFilenameForMimeType(audio.mimeType),
              } as any);
              response = await apiClient.post(`/api/sessions/${activeSessionId}/answers/upload`, formData, { timeoutMs: 45000 });
            }
          } catch (uploadError) {
            console.warn('[InterviewRoom] Answer submit/upload failed, falling back to base64 answer submit:', uploadError);
            const uploadErrorMessage = String((uploadError as any)?.message || '');
            if (/timed out|abort/i.test(uploadErrorMessage)) {
              throw uploadError;
            }
            setProcessingStage('Transcribing');
            const base64 = audio.base64 || await readAudioAsBase64(audio.uri);
            response = await apiClient.post(`/api/sessions/${activeSessionId}/answers`, {
              question_id: currentQuestion.number,
              audio_base64: base64,
              question_text: currentQuestionText,
              audio_mime_type: audio.mimeType,
              audio_duration_seconds: audio.durationSeconds,
              audio_base64_chars: base64.length,
            }, { timeoutMs: 45000 });
            audio.base64Chars = base64.length;
          }
        }

        feedback = response.data;
        clearProcessingTimers();
        setProcessingStage('Transcribing');
        if (feedback.retry_required) {
          setLastFeedback({
            score: 0,
            feedback: feedback.analysis?.feedback || 'We could not detect a clear answer. Please retry this question and speak close to the microphone.',
            transcript: feedback.transcript || '',
            presenceMetrics,
            presenceAttempted,
          });
          setInterviewerReaction(getInterviewerReaction({
            phase: 'processing',
            score: 0,
            feedback: feedback.analysis?.feedback,
            personality: currentPersona?.personality,
          }));
          finishingAnswerRef.current = false;
          setIsFinishingAnswer(false);
          clearProcessingTimers();
          setProcessingStage('');
          return;
        }

        // Store the answer result
        addAnswer({
          questionId: currentQuestion.number,
          questionText: currentQuestionText,
          transcript: feedback.transcript || '',
          score: feedback.analysis?.score || 0,
          clarity: feedback.analysis?.clarity_score || 0,
          pacing: feedback.analysis?.pacing_score || 0,
          impact: feedback.analysis?.impact_score || 0,
          confidence: feedback.analysis?.confidence_score || 0,
          knowledge: feedback.analysis?.knowledge_score || 0,
          feedback: feedback.analysis?.feedback || '',
          tips: feedback.analysis?.tips || [],
          improvedAnswer: feedback.analysis?.improved_answer || '',
          answerStructure: feedback.analysis?.answer_structure || '',
          missingEvidence: feedback.analysis?.missing_evidence || [],
          strongerPhrasing: feedback.analysis?.stronger_phrasing || [],
          presenceMetrics,
          coachingBreakdown: feedback.analysis?.coaching_breakdown,
        });

        if (__DEV__) {
          const serverTimings = feedback.performance?.timings_ms || {};
          console.log('[InterviewRoom] Answer scored:', feedback.analysis?.score);
          console.log('[InterviewRoom] Answer processing timing:', {
            stopRecordingMs: recordingStoppedAt - processingStartedAt,
            backendMs: Date.now() - analysisStartedAt,
            totalMs: Date.now() - processingStartedAt,
            audioChars: audio.base64Chars || 0,
            audioDurationSeconds: audio.durationSeconds,
            localWhisperMs: localTranscriptionMs,
            transcriptSource: localTranscriptSource || 'backend',
            serverTimings,
          });
        }
        const serverTimings = feedback.performance?.timings_ms || {};
        trackEvent('answer_scored', {
          questionNumber: currentQuestion.number,
          score: feedback.analysis?.score || 0,
          clarity: feedback.analysis?.clarity_score || 0,
          pacing: feedback.analysis?.pacing_score || 0,
          impact: feedback.analysis?.impact_score || 0,
          confidence: feedback.analysis?.confidence_score || 0,
          knowledge: feedback.analysis?.knowledge_score || 0,
          stop_recording_ms: recordingStoppedAt - processingStartedAt,
          backend_ms: Date.now() - analysisStartedAt,
          total_ms: Date.now() - processingStartedAt,
          audio_duration_seconds: audio.durationSeconds,
          audio_base64_chars: audio.base64Chars || 0,
          local_whisper_ms: localTranscriptionMs,
          transcript_source: localTranscriptSource || 'backend',
          server_timings_ms: serverTimings,
          server_analysis_ms: serverTimings.analysis,
          server_save_answer_ms: serverTimings.save_answer,
          server_audio_read_ms: serverTimings.audio_read,
          server_audio_encode_ms: serverTimings.audio_encode,
        });
      } catch (err) {
        console.error('[InterviewRoom] Error submitting answer:', err);
        clearProcessingTimers();
        setLastFeedback({
          score: 0,
          feedback: 'We could not analyze this answer because the connection dropped or the scoring service did not respond. Please retry this question.',
          transcript: '',
          presenceMetrics,
          presenceAttempted,
        });
        setInterviewerReaction(getInterviewerReaction({
          phase: 'processing',
          score: 0,
          feedback: 'The answer could not be analyzed.',
          personality: currentPersona?.personality,
        }));
        finishingAnswerRef.current = false;
        setIsFinishingAnswer(false);
        clearProcessingTimers();
        setProcessingStage('');
        return;
      }
    }

    // Show brief feedback before moving on
    if (feedback?.analysis) {
      const score = feedback.analysis?.score || 0;
      if (retryBaseline?.questionNumber === currentQuestion.number) {
        feedback.analysis.retry_comparison = {
          previous_score: retryBaseline.score,
          latest_score: score,
          delta: score - retryBaseline.score,
          previous_transcript: retryBaseline.transcript,
          latest_transcript: feedback.transcript || '',
        };
        setRetryBaseline(null);
      }
      setLastFeedback({
        ...feedback.analysis,
        transcript: feedback.transcript || feedback.analysis?.transcript || '',
        presenceMetrics,
        presenceAttempted,
      });
      setInterviewerReaction(getInterviewerReaction({
        phase: 'processing',
        score: feedback.analysis?.score,
        feedback: feedback.analysis?.feedback,
        followUp: feedback.analysis?.follow_up,
        personality: currentPersona?.personality,
      }));
      if (score < 70) {
        trackEvent('question_retry_offered', {
          questionNumber: currentQuestion.number,
          score,
        });
      }
      finishingAnswerRef.current = false;
      setIsFinishingAnswer(false);
      clearProcessingTimers();
      setProcessingStage('');
      return;
    }

    await advanceQuestion();
    finishingAnswerRef.current = false;
    setIsFinishingAnswer(false);
    clearProcessingTimers();
    setProcessingStage('');
  };

  const handlePresenceMetrics = React.useCallback((metrics: PresenceMetrics) => {
    presenceCameraFrameActiveRef.current = true;
    setPresenceCameraFrameActive(true);
    presenceSamplesRef.current = [...presenceSamplesRef.current.slice(-59), metrics];
    setPresenceSnapshot(metrics);
    setPresenceCameraError(null);
    setPresenceCoachTimedOut(false);
    setPresenceCameraBlocked(false);
  }, []);

  const handlePresenceStatus = React.useCallback(() => {
    presenceCameraFrameActiveRef.current = true;
    setPresenceCameraFrameActive(true);
    setPresenceCameraError(null);
    setPresenceCoachTimedOut(false);
    setPresenceCameraBlocked(false);
  }, []);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleExitInterview = async () => {
    speechRunRef.current += 1;
    setQuestionAudioActive(false);
    setDuixSpeechSource(undefined);
    readyDuixSpeechSourceRef.current.clear();
    setTurnCueActive(false);
    setInterviewerTurnReady(false);
    setIsInterviewerPaused(false);
    stopAudio();

    if (phase === 'answering') {
      try {
        await localWhisperCaptureRef.current?.stop();
        localWhisperCaptureRef.current = null;
        await stopRecording({ includeBase64: false });
      } catch (error) {
        console.warn('[InterviewRoom] Failed to stop recording while exiting:', error);
      }
    }

    router.replace('/(tabs)/home' as any);
  };

  return (
    <SafeAreaView style={styles.container}>
      <LinearGradient
        colors={['rgba(108, 99, 255, 0.05)', 'transparent']}
        style={StyleSheet.absoluteFill}
      />

      {/* Header / Progress */}
      <View style={styles.header}>
        <View style={styles.progressContainer}>
          {questions.map((_, i) => (
            <View
              key={i}
              style={[
                styles.progressBar,
                i < currentQuestionIndex && styles.progressDone,
                i === currentQuestionIndex && styles.progressActive
              ]}
            />
          ))}
        </View>
        <View style={styles.headerMeta}>
          <Text style={styles.questionCounter}>
            {t.common.step} {currentQuestionIndex + 1} / {questions.length}
          </Text>
          <Pressable onPress={handleExitInterview} style={styles.exitBtn}>
            <Feather name="x" size={20} color="#FFFFFF" />
          </Pressable>
        </View>
      </View>

      {/* Main Stage */}
      <View style={[styles.stage, phase === 'answering' && styles.answeringStage]}>
        {/* Connection Beam */}
        <LinearGradient
          colors={['rgba(108, 99, 255, 0.1)', 'transparent']}
          style={styles.connectionBeam}
        />

          {phase === 'answering' && shouldRenderPresenceCoach ? (
            <View style={styles.answerPresenceStage}>
              <View style={styles.answerCameraCard}>
                <PresenceCameraView
                  key={`presence-camera-${currentQuestionIndex}-${presenceCameraKey}`}
                  active={phase === 'answering' && shouldRunPresenceCoach}
                  showPreview
                  style={styles.answerCamera}
                  onPresenceMetrics={(event) => handlePresenceMetrics(event.nativeEvent)}
                  onPresenceStatus={() => handlePresenceStatus()}
                  onPresenceError={(event) => {
                    console.warn('[PresenceCoach] Camera analyzer error:', event.nativeEvent);
                    const code = event.nativeEvent.code || '';
                    setPresenceCameraError(event.nativeEvent.message || 'Camera presence coach could not start.');
                    setPresenceCoachTimedOut(true);
                    if ([
                      'CAMERA_PERMISSION_MISSING',
                      'CAMERA_START_FAILED',
                      'NO_LIFECYCLE',
                      'MEDIAPIPE_INIT_FAILED',
                    ].includes(code)) {
                      setPresenceCameraBlocked(true);
                    }
                  }}
                />
                <View pointerEvents="none" style={styles.answerCameraTopBar}>
                  <Text style={styles.answerCameraLabel}>Camera Presence Coach</Text>
                  <Text style={styles.answerCameraStatus}>
                    {presencePermissionPending
                      ? 'Requesting camera access...'
                      : presenceCameraError
                        ? presenceCameraError
                        : presenceSnapshot?.faceVisible
                          ? 'Face detected'
                          : presenceSnapshot || presenceCameraFrameActive
                            ? 'Camera active. Move your face into frame'
                            : 'Starting camera. Position your face in frame'}
                  </Text>
                </View>
                {(presencePermissionPending || presenceCameraBlocked || presenceCameraNeedsRetry) ? (
                  <View style={styles.answerCameraPermissionOverlay}>
                    <Feather
                      name={presencePermissionPending ? 'loader' : presenceCameraBlocked ? 'video-off' : 'refresh-cw'}
                      size={26}
                      color="#FFFFFF"
                    />
                    <Text style={styles.answerCameraPermissionTitle}>
                      {presencePermissionPending
                        ? 'Opening camera...'
                        : presenceCameraBlocked
                          ? 'Camera access needed'
                          : 'Restart camera'}
                    </Text>
                    <Text style={styles.answerCameraPermissionText}>
                      {presenceCameraError || 'Allow camera access so Prezzence can analyze eye contact, posture, and head position on this device.'}
                    </Text>
                    {!presencePermissionPending ? (
                      <Pressable style={styles.answerCameraPermissionButton} onPress={handlePresencePermissionRetry}>
                        <Text style={styles.answerCameraPermissionButtonText}>
                          {presenceCameraBlocked ? 'Allow camera' : 'Restart camera'}
                        </Text>
                      </Pressable>
                    ) : null}
                  </View>
                ) : null}
                {!presencePermissionPending && !presenceCameraBlocked && !presenceCameraNeedsRetry && !presenceSnapshot?.faceVisible ? (
                  <View pointerEvents="none" style={styles.answerCameraStartingOverlay}>
                    <ActivityIndicator size="small" color={Colors.success} />
                    <Text style={styles.answerCameraStartingText}>
                      {presenceCameraFrameActive ? 'Keep your face in frame' : 'Starting camera'}
                    </Text>
                  </View>
                ) : null}
                {currentPersona && activePersonaCopy ? (
                  <View pointerEvents="none" style={styles.answerInterviewerChip}>
                    <Image
                      source={getLocalPersonaImage(currentPersona, language)}
                      style={styles.answerInterviewerImage}
                      contentFit="cover"
                      contentPosition="top center"
                    />
                    <View style={styles.answerInterviewerCopy}>
                      <Text style={styles.answerInterviewerLabel}>Asked by</Text>
                      <Text style={styles.answerInterviewerName} numberOfLines={1}>
                        {activePersonaCopy.displayName}
                      </Text>
                    </View>
                  </View>
                ) : null}
                <View pointerEvents="none" style={styles.answerPresenceMetrics}>
                  <MetricPill label="Face" value={presenceSnapshot?.faceVisibility} />
                  <MetricPill label="Eyes" value={presenceSnapshot?.eyeContact} />
                  <MetricPill label="Head" value={presenceSnapshot?.headStability} />
                  <MetricPill label="Posture" value={presenceSnapshot?.posture} />
                  <MetricPill label="Energy" value={presenceSnapshot?.expressionEnergy} />
                </View>
              </View>
            </View>
          ) : (
            <AvatarPanel
              personas={activeStagePersonas}
              activePersonaId={currentPersona?.id}
              isUserSpeaking={phase === 'answering'}
              isInterviewerSpeaking={questionAudioActive}
              language={language}
              emotion={interviewerReaction.emotion}
              emotionLabel={questionAudioActive ? 'Asking' : interviewerReaction.label}
              reactionText={interviewerReaction.reaction}
              speechSource={duixSpeechSource}
              isAvatarLoading={shouldWaitForDuix && readyDuixModelName !== activeDuixModelName}
              useDuixAvatar={shouldUseDuixForSession}
              onDuixReady={(modelName) => {
                if (modelName === activeDuixModelName) {
                  setReadyDuixModelName(modelName);
                }
              }}
              onDuixError={(modelName, message) => {
                console.warn('[InterviewRoom] Duix model failed:', modelName, message);
                if (modelName === activeDuixModelName) {
                  setReadyDuixModelName(modelName);
                }
              }}
              onDuixSpeechReady={(source, modelName, durationMs) => {
                if (!modelName || modelName === activeDuixModelName) {
                  markDuixSpeechReady(source, durationMs);
                }
              }}
              onDuixSpeechEnd={(source, modelName) => {
                if (!modelName || modelName === activeDuixModelName) {
                  markDuixSpeechEnded(source);
                }
              }}
              onDuixSpeechError={(source, modelName, message) => {
                console.warn('[InterviewRoom] Duix speech prep failed:', modelName, message);
                markDuixSpeechFailed(source);
              }}
            />
          )}
        <View style={styles.statusStrip}>
          <Text style={styles.userStatus}>
            {phase === 'speaking'
              ? isInterviewerPaused ? 'PAUSED' : interviewerTurnReady || turnCueActive ? 'START SPEAKING NOW' : 'INTERVIEWER SPEAKING'
              : phase === 'answering'
                ? isRecordingPaused ? 'ANSWER PAUSED' : 'YOU ARE SPEAKING'
                : 'READY'}
          </Text>
        </View>
        {phase === 'speaking' && (
          <View style={styles.inlineSpeakingControls}>
            <View style={styles.questionAssistRow}>
              <Pressable
                onPress={isInterviewerPaused ? handleResumeInterviewer : handlePauseInterviewer}
                style={({ pressed }) => [
                  styles.secondaryActionBtn,
                  styles.questionAssistBtn,
                  pressed && { opacity: 0.82 }
                ]}
              >
                <Feather name={isInterviewerPaused ? 'play' : 'pause'} size={16} color="#FFFFFF" />
                <Text style={styles.secondaryActionText}>
                  {isInterviewerPaused ? 'Continue' : 'Pause'}
                </Text>
              </Pressable>
              <Pressable
                onPress={handleRepeatQuestion}
                style={({ pressed }) => [
                  styles.secondaryActionBtn,
                  styles.questionAssistBtn,
                  pressed && { opacity: 0.82 }
                ]}
              >
                <Feather name="rotate-ccw" size={16} color="#FFFFFF" />
                <Text style={styles.secondaryActionText}>Repeat</Text>
              </Pressable>
              <Pressable
                onPress={handleClarifyQuestion}
                style={({ pressed }) => [
                  styles.secondaryActionBtn,
                  styles.questionAssistBtn,
                  pressed && { opacity: 0.82 }
                ]}
              >
                <Feather name="help-circle" size={16} color="#FFFFFF" />
                <Text style={styles.secondaryActionText}>Clarify</Text>
              </Pressable>
            </View>
            <Pressable
              onPress={handleAnswerBegin}
              disabled={!canBeginAnswer || isStartingAnswer || isFinishingAnswer}
              style={[styles.actionBtn, (!canBeginAnswer || isStartingAnswer || isFinishingAnswer) && { opacity: 0.65 }]}
            >
              <Text style={styles.actionBtnText}>
                {isStartingAnswer ? 'Starting...' : canBeginAnswer ? t.interview.answerNow : 'Listening...'}
              </Text>
              <Feather name="mic" size={20} color="#FFFFFF" />
            </Pressable>
          </View>
        )}
        {/* Micro-feedback modal */}
        <Modal
          visible={Boolean(lastFeedback)}
          transparent
          animationType="slide"
          statusBarTranslucent
          onRequestClose={() => {}}
        >
          {lastFeedback ? (
            <View style={styles.feedbackModalBackdrop}>
              <View style={styles.feedbackOverlay}>
                <View style={styles.feedbackHandle} />
            <ScrollView
              style={styles.feedbackScroll}
              contentContainerStyle={styles.feedbackScrollContent}
              keyboardShouldPersistTaps="handled"
              showsVerticalScrollIndicator
              nestedScrollEnabled
              overScrollMode="always"
            >
              <View style={styles.feedbackHero}>
                <View style={styles.feedbackScoreBadge}>
                  <Text style={styles.feedbackScore}>{lastFeedback.score}</Text>
                  <Text style={styles.feedbackLabel}>
                    Score /100
                  </Text>
                </View>
                <View style={styles.feedbackHeroCopy}>
                  <Text style={styles.feedbackHeroTitle}>Answer result</Text>
                  <Text style={styles.feedbackText}>{lastFeedback.feedback}</Text>
                </View>
              </View>
              <View style={styles.transcriptBox}>
                <Text style={styles.transcriptLabel}>Your answer</Text>
                <Text style={styles.transcriptText}>
                  {lastFeedback.transcript || 'No clear transcript was captured for this answer.'}
                </Text>
              </View>
              {lastFeedback.improved_answer ? (
                <View style={styles.improvedAnswerBox}>
                  <View style={styles.improvedAnswerHeader}>
                    <Text style={styles.improvedAnswerLabel}>Stronger answer</Text>
                    <Pressable onPress={() => playCoachingAudio(lastFeedback.improved_answer)} style={styles.listenBtn}>
                      <Feather name="volume-2" size={14} color="#FFFFFF" />
                      <Text style={styles.listenText}>Listen</Text>
                    </Pressable>
                  </View>
                  <InlineHighlightedText text={lastFeedback.improved_answer} style={styles.improvedAnswerText} />
                </View>
              ) : null}
              <PresenceSummaryCard metrics={lastFeedback.presenceMetrics} attempted={lastFeedback.presenceAttempted} />
              <CoachingBreakdown feedback={lastFeedback} />
              {lastFeedback.follow_up ? (
                <Text style={styles.followUpText}>{lastFeedback.follow_up}</Text>
              ) : null}
              {lastFeedback.retry_comparison ? (
                <View style={styles.retryCompareBox}>
                  <Text style={styles.improvedAnswerLabel}>Retry comparison</Text>
                  <Text style={styles.retryCompareText}>
                    {lastFeedback.retry_comparison.delta >= 0 ? '+' : ''}{lastFeedback.retry_comparison.delta} points vs last attempt
                  </Text>
                </View>
              ) : null}
            </ScrollView>
                <View style={styles.feedbackActions}>
                  {(lastFeedback.score || 0) < 70 ? (
                    <Pressable onPress={retryCurrentQuestion} style={styles.retryBtn}>
                      <Feather name="rotate-ccw" size={15} color="#FFFFFF" />
                      <Text style={styles.retryText}>Retry question</Text>
                    </Pressable>
                  ) : null}
                  <Pressable onPress={advanceQuestion} style={styles.continueBtn}>
                    <Text style={styles.continueText}>Continue</Text>
                  </Pressable>
                </View>
              </View>
            </View>
          ) : null}
        </Modal>
      </View>

      {/* Controls */}
      <View style={[styles.footer, phase === 'speaking' && styles.speakingFooter, phase === 'answering' && styles.answerFooter, lastFeedback && styles.footerCollapsed]}>
        {phase === 'answering' && (
          <View style={styles.controlStack}>
            <View style={styles.answerRecorderBar}>
              <Text style={styles.answerTimer}>{formatTime(duration)}</Text>
              <View style={styles.answerWaveformRow}>
                {[1, 2, 4, 3, 5, 2, 4, 3, 1].map((h, i) => (
                  <View key={i} style={[styles.answerWaveBar, { height: h * 6 }]} />
                ))}
              </View>
            </View>
            <View style={styles.answerActionRow}>
              <Pressable
                onPress={isRecordingPaused ? handleResumeAnswer : handlePauseAnswer}
                style={[styles.secondaryActionBtn, styles.answerSecondaryBtn]}
              >
                <Feather name={isRecordingPaused ? 'play' : 'pause'} size={17} color="#FFFFFF" />
                <Text style={styles.secondaryActionText}>
                  {isRecordingPaused ? 'Continue' : 'Pause'}
                </Text>
              </Pressable>
              <Pressable
                onPress={handleAnswerFinish}
                disabled={isFinishingAnswer}
                style={[styles.actionBtn, styles.answerFinishBtn, { backgroundColor: Colors.danger }, isFinishingAnswer && { opacity: 0.65 }]}
              >
                <Text style={styles.actionBtnText}>{isFinishingAnswer ? t.interview.processing : t.common.finish}</Text>
                <Feather name="check-circle" size={20} color="#FFFFFF" />
              </Pressable>
            </View>
          </View>
        )}

        {phase === 'processing' && !lastFeedback && (
          <View style={styles.processingSlot}>
            <View style={styles.processingPanel}>
              <View style={styles.processingIcon}>
                <ActivityIndicator size="small" color={Colors.accent} />
              </View>
              <View style={styles.processingCopy}>
                <Text style={styles.processingTitle}>Transcribing</Text>
              </View>
            </View>
          </View>
        )}
      </View>
    </SafeAreaView>
  );
}

function summarizePresenceSamples(samples: PresenceMetrics[]): PresenceSummary | undefined {
  const usable = samples.filter((sample) => sample.faceVisible);
  if (!usable.length) return undefined;

  return {
    faceVisibility: averageMetric(usable, 'faceVisibility'),
    eyeContact: averageMetric(usable, 'eyeContact'),
    headStability: averageMetric(usable, 'headStability'),
    posture: averageMetric(usable, 'posture'),
    expressionEnergy: averageMetric(usable, 'expressionEnergy'),
    sampleCount: usable.length,
  };
}

function averageMetric(samples: PresenceMetrics[], key: keyof Pick<PresenceMetrics, 'faceVisibility' | 'eyeContact' | 'headStability' | 'posture' | 'expressionEnergy'>) {
  const sum = samples.reduce((total, sample) => total + Number(sample[key] || 0), 0);
  return Math.round(sum / Math.max(1, samples.length));
}

function MetricPill({ label, value }: { label: string; value?: number }) {
  const display = typeof value === 'number' && Number.isFinite(value) ? Math.round(value) : null;

  return (
    <View style={styles.presenceMetricPill}>
      <Text style={styles.presenceMetricLabel}>{label}</Text>
      <Text style={styles.presenceMetricValue}>{display == null ? '--' : display}</Text>
    </View>
  );
}

function PresenceSummaryCard({ metrics, attempted }: { metrics?: PresenceSummary; attempted?: boolean }) {
  if (!metrics?.sampleCount) {
    if (!attempted) return null;

    return (
      <View style={styles.presenceSummaryBox}>
        <Text style={styles.improvedAnswerLabel}>Camera presence</Text>
        <Text style={styles.presenceSummaryWarning}>
          Not enough camera signal was captured for this answer. Keep your face in frame after tapping Answer Now.
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.presenceSummaryBox}>
      <Text style={styles.improvedAnswerLabel}>Camera presence</Text>
      <View style={styles.presenceSummaryGrid}>
        <MetricPill label="Face" value={metrics.faceVisibility} />
        <MetricPill label="Eyes" value={metrics.eyeContact} />
        <MetricPill label="Head" value={metrics.headStability} />
        <MetricPill label="Posture" value={metrics.posture} />
        <MetricPill label="Energy" value={metrics.expressionEnergy} />
      </View>
      <Text style={styles.presenceSummaryNote}>
        Reviewed from {metrics.sampleCount} camera samples.
      </Text>
    </View>
  );
}

function resolveQuestionPersona(personas: Persona[], interviewerName?: string | null, fallbackIndex = 0) {
  const safePersonas = personas.filter(Boolean);
  if (!safePersonas.length) return undefined as unknown as Persona;

  const interviewer = normalizePersonaToken(interviewerName);
  if (interviewer) {
    const directMatch = safePersonas.find((persona) => {
      const tokens = [
        persona.id,
        persona.name,
        persona.role,
        persona.personality,
        persona.badge,
      ].map(normalizePersonaToken).filter(Boolean);
      return tokens.some((token) => interviewer.includes(token) || token.includes(interviewer));
    });
    if (directMatch) return directMatch;
  }

  return safePersonas[Math.abs(fallbackIndex) % safePersonas.length];
}

function normalizePersonaToken(value?: string | null) {
  return (value || '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

function cleanQuestionText(text: string, personas: Array<{ name?: string; id?: string }>, interviewerName?: string) {
  let cleaned = (text || '').trim();
  const names = [
    interviewerName,
    ...personas.map((persona) => persona.name),
    ...personas.map((persona) => persona.id),
    'Maya',
    'Jonas',
    'Sophia',
  ]
    .filter((name): name is string => Boolean(name && name.trim()))
    .sort((a, b) => b.length - a.length);

  for (const name of names) {
    const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    cleaned = cleaned.replace(new RegExp(`([\\s,;:\\-]+)${escaped}\\s*([?.!])?$`, 'i'), (_match, _prefix, punctuation) => punctuation || '?').trim();
  }

  return cleaned.replace(/\s+([?.!,;:])/g, '$1');
}

function buildClarifiedQuestion(question: string) {
  const normalized = question.trim().replace(/\s+/g, ' ');
  const lower = normalized.toLowerCase();

  if (lower.includes('tell me about a time') || lower.includes('describe a time')) {
    return 'Let me ask that more simply. Share one real example. What happened, what did you personally do, and what was the result?';
  }

  if (lower.startsWith('how would you') || lower.includes('how would you')) {
    return 'Let me make that clearer. Walk me through your approach step by step. What would you do first, what would you consider, and how would you know it worked?';
  }

  if (lower.startsWith('how do you') || lower.includes('how do you')) {
    return 'Let me make that clearer. Explain your usual approach in simple steps, then give one example that shows how you use it.';
  }

  if (lower.startsWith('why') || lower.includes('why do you')) {
    return 'Let me simplify that. Give your main reason first, then support it with one specific example or result.';
  }

  return 'Let me make that clearer. Answer with one specific example. Start with the situation, explain what you did, and finish with the result.';
}

function getStartSpeakingCueText(language?: string) {
  const lang = (language || 'en').toLowerCase().split('-')[0];
  const cues: Record<string, string> = {
    en: 'Start speaking now.',
    es: 'Empieza a responder ahora.',
    fr: 'Vous pouvez repondre maintenant.',
    de: 'Bitte antworten Sie jetzt.',
    it: 'Puoi iniziare a rispondere ora.',
    pt: 'Comece a responder agora.',
    zh: '现在开始回答。',
    ja: '今から回答してください。',
    ko: '지금 답변을 시작하세요.',
    ar: 'ابدأ الإجابة الآن.',
    hi: 'अब जवाब देना शुरू करें।',
  };
  return cues[lang] || cues.en;
}

function CoachingBreakdown({ feedback }: { feedback: any }) {
  const breakdown = feedback?.coaching_breakdown || {};
  const items = [
    { label: 'What', value: breakdown.what_to_include },
    { label: 'How', value: breakdown.how_to_structure || feedback?.answer_structure },
    { label: 'Why', value: breakdown.why_it_works },
  ].filter((item) => String(item.value || '').trim());

  if (!items.length) return null;

  return (
    <View style={styles.coachingBox}>
      <Text style={styles.improvedAnswerLabel}>Answer coaching</Text>
      {items.map((item) => (
        <View key={item.label} style={styles.coachingRow}>
          <Text style={styles.coachingLabel}>{item.label}</Text>
          <InlineHighlightedText text={String(item.value)} style={styles.coachingText} />
        </View>
      ))}
    </View>
  );
}

function InlineHighlightedText({ text, style }: { text: string; style: any }) {
  const parts = String(text || '').split(/(\[[^\]]+\])/g).filter(Boolean);

  return (
    <Text style={style}>
      {parts.map((part, index) => {
        const isPlaceholder = /^\[[^\]]+\]$/.test(part);
        return (
          <Text key={`${part}-${index}`} style={isPlaceholder ? styles.placeholderText : undefined}>
            {part}
          </Text>
        );
      })}
    </Text>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bgPrimary,
  },
  header: {
    paddingHorizontal: 22,
    paddingTop: 12,
    gap: 10,
  },
  progressContainer: {
    flexDirection: 'row',
    gap: 6,
  },
  progressBar: {
    flex: 1,
    height: 4,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 2,
  },
  progressActive: {
    backgroundColor: Colors.accent,
  },
  progressDone: {
    backgroundColor: Colors.success,
  },
  headerMeta: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  questionCounter: {
    color: Colors.textSecondary,
    fontSize: 14,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  exitBtn: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  stage: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'flex-start',
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 2,
    gap: 10,
  },
  answeringStage: {
    flex: 0,
    paddingBottom: 0,
    gap: 8,
  },
  connectionBeam: {
    position: 'absolute',
    top: 100,
    bottom: 100,
    width: 2,
    left: '50%',
    marginLeft: -1,
  },
  peerCard: {
    width: '100%',
    borderRadius: 28,
    backgroundColor: 'rgba(28, 28, 46, 0.92)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.28)',
    padding: 22,
    alignItems: 'center',
    gap: 10,
  },
  peerIcon: {
    width: 58,
    height: 58,
    borderRadius: 22,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 2,
  },
  peerTitle: {
    color: '#FFFFFF',
    fontSize: 22,
    fontWeight: '900',
    textAlign: 'center',
  },
  peerBody: {
    color: Colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
    textAlign: 'center',
    maxWidth: 290,
  },
  answerPresenceStage: {
    width: '100%',
  },
  answerCameraCard: {
    width: '100%',
    height: 274,
    borderRadius: 24,
    overflow: 'hidden',
    backgroundColor: '#050509',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.72)',
  },
  answerCamera: {
    width: '100%',
    height: '100%',
  },
  answerCameraTopBar: {
    position: 'absolute',
    top: 10,
    left: 10,
    right: 10,
    minHeight: 42,
    borderRadius: 16,
    backgroundColor: 'rgba(0, 0, 0, 0.62)',
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  answerCameraLabel: {
    color: Colors.success,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1.4,
    textTransform: 'uppercase',
  },
  answerCameraStatus: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '800',
    marginTop: 2,
  },
  answerCameraPermissionOverlay: {
    position: 'absolute',
    left: 20,
    right: 20,
    top: 70,
    bottom: 76,
    borderRadius: 22,
    backgroundColor: 'rgba(0, 0, 0, 0.74)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.14)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
  },
  answerCameraPermissionTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
    marginTop: 10,
    textAlign: 'center',
  },
  answerCameraPermissionText: {
    color: Colors.textSecondary,
    fontSize: 12,
    lineHeight: 17,
    fontWeight: '700',
    textAlign: 'center',
    marginTop: 6,
  },
  answerCameraPermissionButton: {
    minHeight: 38,
    borderRadius: 16,
    backgroundColor: Colors.accent,
    paddingHorizontal: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 12,
  },
  answerCameraPermissionButtonText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
  },
  answerCameraStartingOverlay: {
    position: 'absolute',
    left: 52,
    right: 52,
    top: 108,
    minHeight: 72,
    borderRadius: 18,
    backgroundColor: 'rgba(0, 0, 0, 0.66)',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 9,
  },
  answerCameraStartingText: {
    color: '#FFFFFF',
    fontSize: 14,
    lineHeight: 18,
    fontWeight: '900',
    textAlign: 'center',
  },
  answerInterviewerChip: {
    position: 'absolute',
    left: 10,
    bottom: 66,
    minWidth: 144,
    maxWidth: '62%',
    height: 48,
    borderRadius: 18,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    flexDirection: 'row',
    alignItems: 'center',
    padding: 6,
    gap: 8,
  },
  answerInterviewerImage: {
    width: 36,
    height: 36,
    borderRadius: 14,
  },
  answerInterviewerCopy: {
    flex: 1,
    minWidth: 0,
  },
  answerInterviewerLabel: {
    color: Colors.textSecondary,
    fontSize: 8,
    fontWeight: '900',
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  answerInterviewerName: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
    marginTop: 1,
  },
  answerPresenceMetrics: {
    position: 'absolute',
    left: 10,
    right: 10,
    bottom: 10,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 7,
  },
  statusStrip: {
    minHeight: 24,
    borderRadius: 12,
    paddingHorizontal: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  userStatus: {
    color: Colors.textSecondary,
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 2,
    textTransform: 'uppercase',
  },
  recorderOverlay: {
    alignItems: 'center',
    gap: 8,
    marginTop: -2,
  },
  presenceCoachCard: {
    width: '100%',
    minHeight: 104,
    borderRadius: 22,
    backgroundColor: 'rgba(28, 28, 46, 0.78)',
    borderWidth: 1,
    borderColor: 'rgba(0, 214, 143, 0.22)',
    flexDirection: 'row',
    alignItems: 'center',
    padding: 10,
    gap: 12,
  },
  presenceCameraFrame: {
    width: 82,
    height: 82,
    borderRadius: 18,
    overflow: 'hidden',
    backgroundColor: '#050509',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.12)',
  },
  presenceCamera: {
    width: '100%',
    height: '100%',
  },
  presenceCoachInfo: {
    flex: 1,
    gap: 6,
  },
  presenceCoachLabel: {
    color: Colors.success,
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1.2,
    textTransform: 'uppercase',
  },
  presenceCoachStatus: {
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '800',
  },
  presenceMetricRow: {
    flexDirection: 'row',
    gap: 6,
  },
  presenceMetricPill: {
    flex: 1,
    minWidth: 54,
    minHeight: 38,
    borderRadius: 12,
    backgroundColor: 'rgba(255,255,255,0.06)',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
  },
  presenceMetricLabel: {
    color: Colors.textSecondary,
    fontSize: 9,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  presenceMetricValue: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
    marginTop: 1,
  },
  presenceSummaryBox: {
    marginTop: 14,
    borderRadius: 18,
    padding: 14,
    backgroundColor: 'rgba(0, 214, 143, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(0, 214, 143, 0.22)',
    gap: 10,
  },
  presenceSummaryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  presenceSummaryNote: {
    color: Colors.textSecondary,
    fontSize: 12,
    lineHeight: 17,
    fontWeight: '700',
  },
  presenceSummaryWarning: {
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '800',
  },
  timer: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: '900',
  },
  waveformRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    height: 60,
  },
  waveBar: {
    width: 4,
    backgroundColor: Colors.accent,
    borderRadius: 2,
  },
  footer: {
    paddingHorizontal: 22,
    paddingTop: 4,
    paddingBottom: 18,
  },
  speakingFooter: {
    paddingTop: 0,
    paddingBottom: 0,
  },
  answerFooter: {
    paddingTop: 2,
  },
  controlStack: {
    gap: 8,
  },
  inlineSpeakingControls: {
    width: '100%',
    gap: 12,
    marginTop: 12,
    paddingTop: 2,
  },
  answerRecorderBar: {
    minHeight: 38,
    borderRadius: 18,
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 14,
    paddingHorizontal: 14,
  },
  answerTimer: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
  },
  answerWaveformRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    height: 30,
  },
  answerWaveBar: {
    width: 3,
    backgroundColor: Colors.accent,
    borderRadius: 2,
  },
  answerActionRow: {
    flexDirection: 'row',
    gap: 10,
  },
  answerSecondaryBtn: {
    flex: 0.85,
    height: 54,
    borderRadius: 27,
  },
  answerFinishBtn: {
    flex: 1.15,
    height: 54,
  },
  questionAssistRow: {
    flexDirection: 'row',
    gap: 8,
  },
  questionAssistBtn: {
    flex: 1,
    minHeight: 42,
  },
  secondaryActionBtn: {
    minHeight: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  secondaryActionDisabled: {
    opacity: 0.45,
  },
  secondaryActionText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
  },
  actionBtn: {
    backgroundColor: Colors.accent,
    height: 56,
    borderRadius: 28,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    shadowColor: Colors.accent,
    shadowOpacity: 0.4,
    shadowRadius: 15,
    elevation: 10,
  },
  actionBtnText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
  },
  processingSlot: {
    flex: 1,
    width: '100%',
    justifyContent: 'center',
    paddingTop: 20,
    paddingBottom: 88,
  },
  processingPanel: {
    alignSelf: 'center',
    width: '100%',
    maxWidth: 320,
    backgroundColor: 'rgba(28, 28, 46, 0.94)',
    minHeight: 92,
    borderRadius: 24,
    paddingHorizontal: 20,
    paddingVertical: 18,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.28)',
    shadowColor: '#000',
    shadowOpacity: 0.28,
    shadowRadius: 18,
    elevation: 14,
  },
  processingIcon: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: 'rgba(108, 99, 255, 0.12)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  processingCopy: {
    flex: 1,
    gap: 4,
  },
  processingTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
  },
  processingSubtitle: {
    color: Colors.textSecondary,
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '700',
  },
  feedbackModalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.68)',
    justifyContent: 'flex-end',
    paddingHorizontal: 12,
    paddingTop: 54,
    paddingBottom: Platform.OS === 'android' ? 12 : 22,
  },
  feedbackOverlay: {
    backgroundColor: 'rgba(18, 18, 29, 0.98)',
    borderRadius: 26,
    alignItems: 'stretch',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.3)',
    overflow: 'hidden',
    maxHeight: '88%',
    minHeight: 420,
    shadowColor: '#000',
    shadowOpacity: 0.35,
    shadowRadius: 22,
    elevation: 18,
  },
  feedbackHandle: {
    alignSelf: 'center',
    width: 42,
    height: 4,
    borderRadius: 2,
    backgroundColor: 'rgba(255, 255, 255, 0.22)',
    marginTop: 10,
    marginBottom: 2,
  },
  feedbackScroll: {
    alignSelf: 'stretch',
    flex: 1,
  },
  feedbackScrollContent: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 12,
    gap: 12,
  },
  feedbackHero: {
    alignSelf: 'stretch',
    flexDirection: 'row',
    gap: 12,
    borderRadius: 20,
    backgroundColor: 'rgba(108, 99, 255, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.24)',
    padding: 14,
  },
  feedbackScoreBadge: {
    width: 86,
    minHeight: 88,
    borderRadius: 18,
    backgroundColor: 'rgba(10, 10, 15, 0.78)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  feedbackHeroCopy: {
    flex: 1,
    minWidth: 0,
    justifyContent: 'center',
  },
  feedbackHeroTitle: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '900',
    marginBottom: 6,
  },
  transcriptBox: {
    alignSelf: 'stretch',
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderRadius: 18,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  transcriptLabel: {
    color: '#FFD166',
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1.2,
    textTransform: 'uppercase',
    marginBottom: 7,
  },
  transcriptText: {
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
  },
  feedbackScore: {
    color: Colors.accent,
    fontSize: 42,
    fontWeight: '900',
    letterSpacing: -2,
  },
  feedbackLabel: {
    color: Colors.textSecondary,
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 1,
    textTransform: 'uppercase',
    marginTop: 2,
    textAlign: 'center',
  },
  feedbackText: {
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 18,
    opacity: 0.85,
  },
  improvedAnswerBox: {
    alignSelf: 'stretch',
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderRadius: 18,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  improvedAnswerHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
    marginBottom: 8,
  },
  improvedAnswerLabel: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1.4,
    textTransform: 'uppercase',
  },
  improvedAnswerText: {
    color: '#E7E7F3',
    fontSize: 13,
    lineHeight: 18,
  },
  coachingBox: {
    alignSelf: 'stretch',
    backgroundColor: 'rgba(0, 214, 143, 0.08)',
    borderRadius: 18,
    padding: 14,
    borderWidth: 1,
    borderColor: 'rgba(0, 214, 143, 0.18)',
    gap: 10,
  },
  coachingRow: {
    gap: 5,
  },
  coachingLabel: {
    color: Colors.success,
    fontSize: 11,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  coachingText: {
    color: '#E7E7F3',
    fontSize: 12,
    lineHeight: 17,
    fontWeight: '600',
  },
  placeholderText: {
    color: '#00D68F',
    fontWeight: '900',
    backgroundColor: 'rgba(0, 214, 143, 0.14)',
  },
  listenBtn: {
    minHeight: 30,
    borderRadius: 15,
    paddingHorizontal: 10,
    backgroundColor: Colors.accent,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  listenText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '900',
  },
  retryCompareBox: {
    alignSelf: 'stretch',
    borderRadius: 16,
    backgroundColor: 'rgba(0, 214, 143, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(0, 214, 143, 0.22)',
    padding: 12,
  },
  retryCompareText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '900',
    marginTop: 4,
  },
  feedbackActions: {
    flexDirection: 'row',
    gap: 10,
    alignSelf: 'stretch',
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 16,
    backgroundColor: 'rgba(18, 18, 29, 0.98)',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.08)',
  },
  retryBtn: {
    flex: 1,
    minHeight: 44,
    borderRadius: 22,
    backgroundColor: Colors.accent,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  retryText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
  },
  continueBtn: {
    flex: 1,
    minHeight: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.12)',
  },
  continueText: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '900',
  },
  followUpText: {
    color: Colors.textSecondary,
    fontSize: 13,
    fontStyle: 'italic',
    lineHeight: 18,
    textAlign: 'center',
  },
  footerCollapsed: {
    height: 0,
    paddingTop: 0,
    paddingBottom: 0,
    overflow: 'hidden',
  },
  recoveryScreen: {
    flex: 1,
    paddingHorizontal: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  recoveryIcon: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: 'rgba(255, 176, 32, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(255, 176, 32, 0.28)',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 32,
    marginBottom: 22,
  },
  recoveryTitle: {
    color: '#FFFFFF',
    fontSize: 25,
    lineHeight: 31,
    fontWeight: '900',
    textAlign: 'center',
  },
  recoveryBody: {
    color: Colors.textSecondary,
    fontSize: 15,
    lineHeight: 23,
    textAlign: 'center',
    marginTop: 12,
  },
  recoveryMeta: {
    color: 'rgba(255, 255, 255, 0.45)',
    fontSize: 11,
    lineHeight: 16,
    textAlign: 'center',
    marginTop: 14,
  },
  recoveryButton: {
    height: 56,
    borderRadius: 28,
    backgroundColor: Colors.accent,
    paddingHorizontal: 28,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 28,
  },
  recoveryButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
  },
});

