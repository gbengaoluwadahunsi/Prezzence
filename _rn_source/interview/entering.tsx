import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Animated, Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useSessionStore } from '../../src/store/sessionStore';
import { useSettingsStore } from '../../src/store/settingsStore';
import { getPersonasForInterview } from '../../src/data/personas';
import { runInterviewDevicePreflight, summarizeDeviceCapabilities } from '../../src/services/devicePreflight';
import { getLocalPersonaImage, getLocalizedPersonaCopy } from '../../src/utils/personaAssets';
import { BrandWordmark } from '../../src/components/BrandWordmark';

const VIOLET = '#5C61FF';
const BG_DARK = '#000000';
const MUTED_TEXT = '#A5A6BA';

const FEMALE_PREVIEW = require('../../assets/images/user_female.png');
const MALE_PREVIEW = require('../../assets/images/user_male.png');
const INTERVIEW_ROOM_PREVIEW = require('../../assets/images/interview_bg.png');
function PersonaImage({
  persona,
  language,
  style,
  resizeMode = 'cover',
}: {
  persona: any;
  language: string;
  style: any;
  resizeMode?: 'cover' | 'contain' | 'stretch' | 'repeat' | 'center';
}) {
  const [useRemoteFallback, setUseRemoteFallback] = useState(false);
  const remoteUrl = persona?.avatarUrl || persona?.avatar_url;
  const source = useRemoteFallback && remoteUrl
    ? { uri: remoteUrl }
    : getLocalPersonaImage(persona, language);

  useEffect(() => {
    setUseRemoteFallback(false);
  }, [persona?.id, persona?.name, language]);

  return (
    <Image
      source={source || FEMALE_PREVIEW}
      style={style}
      resizeMode={resizeMode}
      fadeDuration={0}
      onError={() => {
        if (remoteUrl && !useRemoteFallback) {
          setUseRemoteFallback(true);
        }
      }}
    />
  );
}

export default function EnteringRoomScreen() {
  const {
    userGender,
    selectedPersonas,
    language,
    interviewerMode,
    activeSessionId,
    questions,
    deviceCapabilities,
    setDeviceCapabilities,
  } = useSessionStore();
  const { cameraPresenceCoachEnabled } = useSettingsStore();
  const effectiveInterviewerMode = interviewerMode === 'peer' ? 'single' : interviewerMode;
  const displayPersonas = React.useMemo(() => {
    return selectedPersonas.length ? selectedPersonas : getPersonasForInterview(effectiveInterviewerMode, 'neutral');
  }, [effectiveInterviewerMode, selectedPersonas]);
  const copy = enteringCopy(language);
  const isSingle = effectiveInterviewerMode === 'single' || displayPersonas.length === 1;
  const hasSessionReady = Boolean(activeSessionId && questions.length > 0);
  const hasCurrentDeviceSetup = Boolean(activeSessionId && deviceCapabilities?.sessionId === activeSessionId);
  const [preflightStatus, setPreflightStatus] = useState('');
  const [isPreflightRunning, setIsPreflightRunning] = useState(false);
  const isPreparingSession = !hasSessionReady;

  // Keep fixed animation slots so fallback personas do not render at opacity 0
  // when the persisted panel hydrates after the first render.
  const fadeAnims = useRef([0, 1, 2].map(() => new Animated.Value(1))).current;
  const slideAnims = useRef([0, 1, 2].map(() => new Animated.Value(0))).current;

  useEffect(() => {
    if (displayPersonas.length === 0) return;

    const animations = displayPersonas.map((_, i) =>
      Animated.parallel([
        Animated.timing(fadeAnims[i], { toValue: 1, duration: 600, useNativeDriver: true }),
        Animated.timing(slideAnims[i], { toValue: 0, duration: 600, useNativeDriver: true })
      ])
    );

    Animated.stagger(300, animations).start();
  }, [displayPersonas]);

  const runDeviceSetup = React.useCallback(async () => {
    if (!activeSessionId || questions.length === 0 || isPreflightRunning) return null;

    setIsPreflightRunning(true);
    setPreflightStatus('Preparing your interview room...');
    try {
      const result = await runInterviewDevicePreflight({
        sessionId: activeSessionId,
        language,
        interviewerMode: effectiveInterviewerMode,
        cameraPresenceRequested: cameraPresenceCoachEnabled,
        onStatus: setPreflightStatus,
      });
      setDeviceCapabilities(result);
      setPreflightStatus(summarizeDeviceCapabilities(result));
      return result;
    } catch (error) {
      const fallback = {
        sessionId: activeSessionId,
        checkedAt: Date.now(),
        mode: 'lite' as const,
        avatar: 'static' as const,
        transcription: 'backend' as const,
        presenceCoach: 'disabled' as const,
        reasons: ['Interview room prepared with the standard setup.'],
      };
      setDeviceCapabilities(fallback);
      setPreflightStatus(summarizeDeviceCapabilities(fallback));
      return fallback;
    } finally {
      setIsPreflightRunning(false);
    }
  }, [
    activeSessionId,
    cameraPresenceCoachEnabled,
    effectiveInterviewerMode,
    isPreflightRunning,
    language,
    questions.length,
    setDeviceCapabilities,
  ]);

  useEffect(() => {
    if (!hasSessionReady || hasCurrentDeviceSetup) return;
    runDeviceSetup();
  }, [hasCurrentDeviceSetup, hasSessionReady, runDeviceSetup]);

  const personaText = displayPersonas.length > 0
    ? (!hasSessionReady
      ? copy.preparingQuestions
      : isSingle ? copy.singleReady : copy.readyCount.replace('{0}', String(displayPersonas.length)))
    : copy.preparing;

  const handleJoinInterview = async () => {
    if (!activeSessionId || questions.length === 0) {
      router.replace('/(tabs)/home' as any);
      return;
    }

    router.replace('/interview/speaking');
  };

  const handleExitInterview = () => {
    router.replace('/(tabs)/home' as any);
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.headerNav}>
        <Pressable onPress={handleExitInterview} style={styles.backButton}>
          <Feather name="chevron-left" size={24} color="#FFFFFF" />
        </Pressable>
        <View style={styles.headerCenter}>
          <Text style={styles.title}>{copy.title}</Text>
        </View>
        <View style={styles.headerRight} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.content}>
          <View style={styles.heroLine}>
            <Text style={styles.eyebrow}>{isSingle ? copy.singleEyebrow : copy.eyebrow}</Text>
            <Text style={styles.headline}>{isSingle ? copy.singleHeadline : copy.headline}</Text>
            <Text style={styles.subheadline}>{personaText}</Text>
          </View>

          {isSingle && displayPersonas[0] ? (
            <View style={styles.singleVideoCard}>
              <View style={styles.videoImageStage}>
                {isPreparingSession ? (
                  <View style={styles.videoPreparingStage}>
                    <BrandWordmark size={46} textSize={18} uppercase={false} />
                    <ActivityIndicator color={VIOLET} style={styles.videoPreparingSpinner} />
                    <Text style={styles.videoPreparingTitle}>{copy.preparingVideoTitle}</Text>
                    <Text style={styles.videoPreparingText}>{copy.preparingVideoText}</Text>
                  </View>
                ) : (
                  <Image source={INTERVIEW_ROOM_PREVIEW} style={styles.videoSplashImage} resizeMode="contain" />
                )}
                <View style={styles.videoChrome}>
                  <View style={styles.liveBadge}>
                    <View style={styles.liveDot} />
                    <Text style={styles.liveText}>{copy.singleEyebrow}</Text>
                  </View>
                  <View style={styles.signalPill}>
                    <View style={styles.readyDotInline} />
                    <Text style={styles.signalText}>{isPreparingSession ? copy.preparingStatus : copy.ready}</Text>
                  </View>
                </View>
              </View>
              <View style={styles.videoInfoStrip}>
                <View style={styles.videoTextBlock}>
                  <Text style={styles.videoName} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.76}>
                    {getLocalizedPersonaCopy(displayPersonas[0], language).displayName}
                  </Text>
                  <Text style={styles.videoRole} numberOfLines={1}>
                    {getLocalizedPersonaCopy(displayPersonas[0], language).displayRole}
                  </Text>
                </View>
                <View style={styles.videoFooterRow}>
                  <View style={styles.callControl}>
                    <Feather name="mic" size={14} color="#FFFFFF" />
                  </View>
                  <View style={styles.callControl}>
                    <Feather name="video" size={14} color="#FFFFFF" />
                  </View>
                  <View style={styles.firstQuestionPill}>
                    <Text style={styles.firstQuestionText}>{copy.firstQuestion}</Text>
                  </View>
                </View>
              </View>
            </View>
          ) : (
            <View style={styles.panelRow}>
              {displayPersonas.map((persona, index) => {
                const localized = getLocalizedPersonaCopy(persona, language);
                return (
                  <Animated.View
                    key={persona.id || index}
                    style={[
                      styles.avatarCont,
                      {
                        opacity: fadeAnims[index] || 1,
                        transform: [{ translateY: slideAnims[index] || 0 }]
                      }
                    ]}
                  >
                    <View style={[
                      styles.panelCard,
                      index === 1 && styles.primaryPanelCard
                    ]}>
                      <View style={styles.avatarFrame}>
                        <PersonaImage persona={persona} language={language} style={styles.smallAvatar} />
                        <View style={styles.readyDot} />
                      </View>
                      <Text style={styles.avatarName} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>
                        {localized.displayName}
                      </Text>
                      <Text style={styles.avatarRole} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>
                        {localized.displayRole}
                      </Text>
                      <View style={styles.statusPill}>
                        <Text style={styles.statusText}>{index === 0 ? copy.firstQuestion : copy.ready}</Text>
                      </View>
                    </View>
                  </Animated.View>
                );
              })}
            </View>
          )}

          <View style={styles.setupPreview}>
            <Image
              source={userGender === 'male' ? MALE_PREVIEW : FEMALE_PREVIEW}
              style={styles.setupImage}
              resizeMode="cover"
            />
            <View style={styles.setupCopy}>
              <Text style={styles.setupLabel}>{hasCurrentDeviceSetup ? copy.setupLabel : 'DEVICE SETUP'}</Text>
              <Text style={styles.setupTitle}>
                {hasCurrentDeviceSetup
                  ? summarizeDeviceCapabilities(deviceCapabilities)
                  : preflightStatus || copy.setupTitle}
              </Text>
            </View>
            {isPreflightRunning || !hasCurrentDeviceSetup ? (
              <ActivityIndicator color="#00D68F" />
            ) : (
              <Feather name="check-circle" size={22} color="#00D68F" />
            )}
          </View>
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <Pressable
          onPress={handleJoinInterview}
          disabled={isPreparingSession}
          style={({ pressed }) => [
            styles.continueButton,
            isPreparingSession && styles.continueButtonDisabled,
            pressed && { opacity: 0.8 }
          ]}
        >
          {isPreparingSession ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <>
              <Text style={styles.continueText}>{copy.join}</Text>
              <Feather name="arrow-right" size={18} color="#FFFFFF" style={{ marginLeft: 10 }} />
            </>
          )}
          {isPreparingSession ? (
            <Text style={styles.continueText}>
              {copy.preparingButton}
            </Text>
          ) : null}
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

function enteringCopy(language: string) {
  const en = {
    title: 'Entering the Room',
    eyebrow: 'LIVE PANEL',
    headline: 'Your interviewers are ready.',
    singleEyebrow: 'LIVE INTERVIEW',
    singleHeadline: 'Your interviewer is ready.',
    singleReady: '1 interviewer is ready.',
    readyCount: '{0} interviewers are ready.',
    preparing: 'Preparing your interview panel.',
    firstQuestion: 'First question',
    ready: 'Ready',
    setupLabel: 'Setup preview',
    setupTitle: 'Camera and audio staged',
    preparingQuestions: 'Preparing your questions and interview room.',
    preparingFallback: 'Preparing your questions and interview room.',
    preparingStatus: 'Preparing',
    preparingVideoTitle: 'Preparing room',
    preparingVideoText: 'Preparing your questions and interview room.',
    preparingButton: 'Preparing...',
    preparingFallbackButton: 'Preparing...',
    join: 'Join Interview',
  };
  const copies: Record<string, Partial<typeof en>> = {
    en,
    es: { title: 'Entrando a la sala', eyebrow: 'PANEL EN VIVO', headline: 'Tus entrevistadores están listos.', readyCount: '{0} entrevistadores están listos.', preparing: 'Preparando tu panel de entrevista.', firstQuestion: 'Primera pregunta', ready: 'Listo', setupLabel: 'Vista previa', setupTitle: 'Cámara y audio preparados', join: 'Entrar a la entrevista' },
    fr: { title: 'Entrée dans la salle', eyebrow: 'PANEL EN DIRECT', headline: 'Vos intervieweurs sont prêts.', readyCount: '{0} intervieweurs sont prêts.', preparing: 'Préparation de votre panel.', firstQuestion: 'Première question', ready: 'Prêt', setupLabel: 'Aperçu', setupTitle: 'Caméra et audio prêts', join: 'Rejoindre l’entretien' },
    de: { title: 'Raum betreten', eyebrow: 'LIVE-PANEL', headline: 'Ihre Interviewer sind bereit.', readyCount: '{0} Interviewer sind bereit.', preparing: 'Ihr Interview-Panel wird vorbereitet.', firstQuestion: 'Erste Frage', ready: 'Bereit', setupLabel: 'Setup-Vorschau', setupTitle: 'Kamera und Audio bereit', join: 'Interview starten' },
    it: { title: 'Ingresso nella stanza', eyebrow: 'PANEL LIVE', headline: 'I tuoi intervistatori sono pronti.', readyCount: '{0} intervistatori sono pronti.', preparing: 'Preparazione del panel.', firstQuestion: 'Prima domanda', ready: 'Pronto', setupLabel: 'Anteprima setup', setupTitle: 'Camera e audio pronti', join: 'Entra nel colloquio' },
    pt: { title: 'Entrando na sala', eyebrow: 'PAINEL AO VIVO', headline: 'Seus entrevistadores estão prontos.', readyCount: '{0} entrevistadores estão prontos.', preparing: 'Preparando seu painel.', firstQuestion: 'Primeira pergunta', ready: 'Pronto', setupLabel: 'Prévia do setup', setupTitle: 'Câmera e áudio prontos', join: 'Entrar na entrevista' },
    zh: { title: '进入面试间', eyebrow: '实时面试组', headline: '你的面试官已准备好。', readyCount: '{0} 位面试官已准备好。', preparing: '正在准备你的面试小组。', firstQuestion: '第一题', ready: '就绪', setupLabel: '设置预览', setupTitle: '摄像头和音频已准备', join: '加入面试' },
    ja: { title: '面接ルームに入室', eyebrow: 'ライブパネル', headline: '面接官の準備ができました。', readyCount: '{0} 名の面接官が準備完了です。', preparing: '面接パネルを準備中です。', firstQuestion: '最初の質問', ready: '準備完了', setupLabel: 'セットアップ確認', setupTitle: 'カメラと音声の準備完了', join: '面接に参加' },
    ko: { title: '면접실 입장', eyebrow: '라이브 패널', headline: '면접관이 준비되었습니다.', readyCount: '면접관 {0}명이 준비되었습니다.', preparing: '면접 패널을 준비 중입니다.', firstQuestion: '첫 질문', ready: '준비됨', setupLabel: '설정 미리보기', setupTitle: '카메라와 오디오 준비 완료', join: '면접 참여' },
    ar: { title: 'الدخول إلى الغرفة', eyebrow: 'لجنة مباشرة', headline: 'المقابلون جاهزون.', readyCount: '{0} مقابلين جاهزون.', preparing: 'جار إعداد لجنة المقابلة.', firstQuestion: 'السؤال الأول', ready: 'جاهز', setupLabel: 'معاينة الإعداد', setupTitle: 'الكاميرا والصوت جاهزان', join: 'انضم إلى المقابلة' },
    hi: { title: 'कमरे में प्रवेश', eyebrow: 'लाइव पैनल', headline: 'आपके इंटरव्यूअर तैयार हैं।', readyCount: '{0} इंटरव्यूअर तैयार हैं।', preparing: 'आपका इंटरव्यू पैनल तैयार हो रहा है।', firstQuestion: 'पहला प्रश्न', ready: 'तैयार', setupLabel: 'सेटअप प्रीव्यू', setupTitle: 'कैमरा और ऑडियो तैयार', join: 'इंटरव्यू में जाएं' },
  };
  return { ...en, ...(copies[language] || {}) };
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: BG_DARK,
  },
  headerNav: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    height: 80,
  },
  backButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#1C1C2E',
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerCenter: {
    alignItems: 'center',
  },
  title: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  headerRight: {
    width: 44,
  },
  scrollContent: {
    paddingBottom: 120, // Space for the floating button
  },
  content: {
    paddingHorizontal: 24,
    paddingTop: 30,
  },
  heroLine: {
    alignItems: 'center',
    marginBottom: 28,
  },
  eyebrow: {
    color: VIOLET,
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 2,
    marginBottom: 10,
  },
  headline: {
    color: '#FFFFFF',
    fontSize: 27,
    fontWeight: '900',
    textAlign: 'center',
    lineHeight: 34,
  },
  subheadline: {
    color: MUTED_TEXT,
    fontSize: 14,
    marginTop: 8,
    textAlign: 'center',
    marginBottom: 10,
  },
  panelRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 8,
    marginTop: 10,
    marginBottom: 22,
  },
  avatarCont: {
    alignItems: 'center',
  },
  panelCard: {
    width: 100,
    minHeight: 154,
    alignItems: 'center',
    backgroundColor: '#111119',
    borderRadius: 22,
    paddingHorizontal: 8,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
  },
  primaryPanelCard: {
    borderColor: 'rgba(92, 97, 255, 0.55)',
    backgroundColor: '#151526',
    transform: [{ translateY: -8 }],
  },
  avatarFrame: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: '#1C1C2E',
    borderWidth: 2,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    padding: 3,
    marginBottom: 12,
    position: 'relative',
  },
  smallAvatar: {
    width: '100%',
    height: '100%',
    borderRadius: 33,
    backgroundColor: '#1C1C2E',
  },
  readyDot: {
    position: 'absolute',
    right: 2,
    bottom: 5,
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: '#00D68F',
    borderWidth: 2,
    borderColor: '#111119',
  },
  avatarName: {
    color: '#FFFFFF',
    fontSize: 13,
    fontWeight: '800',
    textAlign: 'center',
    lineHeight: 17,
  },
  avatarRole: {
    color: MUTED_TEXT,
    fontSize: 10,
    lineHeight: 13,
    fontWeight: '700',
    textAlign: 'center',
    marginTop: 3,
    width: '100%',
  },
  statusPill: {
    marginTop: 8,
    minWidth: 70,
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: 12,
    backgroundColor: 'rgba(92, 97, 255, 0.16)',
    alignItems: 'center',
  },
  statusText: {
    color: '#C7C9FF',
    fontSize: 8,
    lineHeight: 10,
    fontWeight: '800',
    textTransform: 'uppercase',
    textAlign: 'center',
  },
  singleVideoCard: {
    width: '100%',
    borderRadius: 22,
    overflow: 'hidden',
    backgroundColor: '#111119',
    borderWidth: 1,
    borderColor: 'rgba(92, 97, 255, 0.7)',
    marginBottom: 24,
  },
  peerReadyCard: {
    width: '100%',
    borderRadius: 26,
    backgroundColor: '#111119',
    borderWidth: 1,
    borderColor: 'rgba(92, 97, 255, 0.55)',
    padding: 24,
    alignItems: 'center',
    marginBottom: 24,
  },
  peerReadyIcon: {
    width: 68,
    height: 68,
    borderRadius: 24,
    backgroundColor: VIOLET,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  peerReadyTitle: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: '900',
    textAlign: 'center',
    marginBottom: 8,
  },
  peerReadyBody: {
    color: MUTED_TEXT,
    fontSize: 14,
    lineHeight: 21,
    textAlign: 'center',
    fontWeight: '700',
    marginBottom: 18,
  },
  peerSteps: {
    alignSelf: 'stretch',
    gap: 10,
  },
  peerStep: {
    color: '#FFFFFF',
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '800',
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 11,
  },
  videoImageStage: {
    width: '100%',
    aspectRatio: 4 / 3,
    backgroundColor: '#050509',
    position: 'relative',
    alignItems: 'center',
    justifyContent: 'center',
  },
  videoChrome: {
    position: 'absolute',
    top: 14,
    left: 14,
    right: 14,
    zIndex: 4,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  liveBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 7,
    backgroundColor: 'rgba(0, 0, 0, 0.58)',
    borderRadius: 16,
    paddingHorizontal: 9,
    paddingVertical: 6,
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#FF3B6B',
  },
  liveText: {
    color: '#FFFFFF',
    fontSize: 9,
    fontWeight: '900',
    letterSpacing: 1.2,
  },
  signalPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    borderRadius: 16,
    paddingHorizontal: 9,
    paddingVertical: 6,
  },
  readyDotInline: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#00D68F',
  },
  signalText: {
    color: '#FFFFFF',
    fontSize: 9,
    fontWeight: '800',
    textTransform: 'uppercase',
  },
  videoAvatar: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#0F0F18',
    zIndex: 1,
  },
  videoSplashImage: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#050509',
    zIndex: 1,
  },
  videoPreparingStage: {
    ...StyleSheet.absoluteFillObject,
    zIndex: 2,
    backgroundColor: '#10101A',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  videoPreparingSpinner: {
    marginTop: 18,
    marginBottom: 8,
  },
  videoPreparingTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    lineHeight: 21,
    fontWeight: '900',
    textAlign: 'center',
  },
  videoPreparingText: {
    color: MUTED_TEXT,
    fontSize: 13,
    lineHeight: 18,
    fontWeight: '700',
    textAlign: 'center',
    marginTop: 4,
  },
  videoInfoStrip: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 14,
    backgroundColor: '#151526',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.08)',
  },
  videoTextBlock: {
    marginBottom: 10,
  },
  videoName: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '900',
  },
  videoRole: {
    color: '#D9DAE8',
    fontSize: 11,
    fontWeight: '700',
    marginTop: 1,
  },
  videoAttire: {
    color: '#B9BCFF',
    fontSize: 9,
    fontWeight: '600',
    marginTop: 3,
  },
  videoFooterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 8,
  },
  callControl: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: 'rgba(255, 255, 255, 0.14)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  firstQuestionPill: {
    flex: 1,
    height: 30,
    borderRadius: 15,
    backgroundColor: 'rgba(92, 97, 255, 0.34)',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
  },
  firstQuestionText: {
    color: '#FFFFFF',
    fontSize: 9,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  setupPreview: {
    flexDirection: 'row',
    alignItems: 'center',
    width: '100%',
    backgroundColor: '#111119',
    borderRadius: 22,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.08)',
    padding: 12,
    marginBottom: 90,
  },
  setupImage: {
    width: 58,
    height: 58,
    borderRadius: 16,
    marginRight: 14,
  },
  setupCopy: {
    flex: 1,
  },
  setupLabel: {
    color: VIOLET,
    fontSize: 9,
    fontWeight: '900',
    letterSpacing: 1.5,
  },
  setupTitle: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '800',
    marginTop: 4,
  },
  footer: {
    paddingHorizontal: 24,
    paddingBottom: 34,
  },
  continueButton: {
    backgroundColor: VIOLET,
    height: 64,
    borderRadius: 32,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  continueButtonDisabled: {
    opacity: 0.72,
    gap: 10,
  },
  continueText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '700',
  },
});
