import { Feather } from '@expo/vector-icons';
import { useLocalSearchParams, router } from 'expo-router';
import React from 'react';
import {
  ActivityIndicator,
  Dimensions,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { getSessionDetail } from '../../src/api/sessions';
import { StrengthBars } from '../../src/components/charts/StrengthBars';
import { useSessionStore } from '../../src/store/sessionStore';
import { exportSessionReportPdf } from '../../src/services/pdfExport';
import { BETA_UNLOCK_ALL_FEATURES } from '../../src/config/featureFlags';

const { width } = Dimensions.get('window');

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  success: '#00D68F',
  warning: '#F59E0B',
  danger: '#FF4757',
  textSecondary: '#9B9BAA',
  card: '#1C1C2E',
};

type SessionAnswer = {
  id?: string;
  question_id: number;
  transcript?: string;
  score?: number;
  feedback?: string;
  tips?: string[];
  improved_answer?: string;
  answer_structure?: string;
  missing_evidence?: string[];
  stronger_phrasing?: string[];
};

type SessionDetail = {
  id: string;
  role_title: string;
  industry: string;
  status: string;
  date: string;
  duration: string;
  analytics: {
    radar_data: Array<{ label: string; value: number }>;
    metrics: Record<string, number>;
  };
  answers: SessionAnswer[];
  feedback: {
    summary?: string;
    strengths: string[];
    growth_areas: string[];
    coaching_plan?: string[];
    strongest_metric?: string | null;
    weakest_metric?: string | null;
    has_signal?: boolean;
    scorable_answers?: number;
  };
  score_band?: {
    label: string;
    description: string;
  };
};

function asTextList(value: unknown): string[] {
  if (!value) return [];
  if (Array.isArray(value)) return value.map(String).filter(Boolean);
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return [];
    try {
      const parsed = JSON.parse(trimmed);
      return asTextList(parsed);
    } catch {
      return [trimmed];
    }
  }
  return [String(value)];
}

function answerScore(answer: SessionAnswer) {
  const numeric = Number(answer.score);
  return Number.isFinite(numeric) ? numeric : 0;
}

function scoreColor(score: number) {
  if (score >= 75) return Colors.success;
  if (score >= 55) return Colors.warning;
  return Colors.danger;
}

export default function SessionReportScreen() {
  const { sessionId } = useLocalSearchParams<{ sessionId: string }>();
  const { isPremium } = useSessionStore();
  const hasPremiumAccess = BETA_UNLOCK_ALL_FEATURES || isPremium;
  const [session, setSession] = React.useState<SessionDetail | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState('');
  const [exportError, setExportError] = React.useState('');

  React.useEffect(() => {
    let mounted = true;

    const load = async () => {
      if (!sessionId) return;
      setLoading(true);
      setError('');
      try {
        const data = await getSessionDetail(String(sessionId));
        if (mounted) setSession(data);
      } catch (err: any) {
        if (mounted) setError(err?.message || 'Unable to load this session report.');
      } finally {
        if (mounted) setLoading(false);
      }
    };

    load();
    return () => {
      mounted = false;
    };
  }, [sessionId]);

  const avgScore = React.useMemo(() => {
    if (!session?.answers?.length) return 0;
    const scorable = session.answers.filter((answer) => answerScore(answer) > 0 && String(answer.transcript || '').trim());
    if (!scorable.length) return 0;
    return Math.round(scorable.reduce((sum, answer) => sum + answerScore(answer), 0) / scorable.length);
  }, [session]);
  const scorableAnswerCount = React.useMemo(() => {
    if (!session?.answers?.length) return 0;
    return session.answers.filter((answer) => answerScore(answer) > 0 && String(answer.transcript || '').trim()).length;
  }, [session]);
  const hasUsableSignal = Boolean(session?.feedback?.has_signal ?? scorableAnswerCount > 0);
  const reportSummary = hasUsableSignal
    ? session?.feedback.summary || 'Complete more answers to build a stronger interview report.'
    : 'Not enough usable speech was captured to produce a real coaching summary. This report is showing setup guidance instead of skill rankings.';
  const coachingPlan = hasUsableSignal
    ? (session?.feedback.coaching_plan?.length ? session.feedback.coaching_plan : ['Use situation, action, tradeoff, and measurable result in your next answer.'])
    : [
      'Retry the interview with the microphone close to your mouth.',
      'Speak for at least 30 seconds before tapping Finish.',
      'Make sure a transcript appears after each answer before continuing.',
    ];

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={styles.circleBtn}>
            <Feather name="chevron-left" size={24} color="#FFFFFF" />
          </Pressable>
          <Text style={styles.headerTitle}>Session Report</Text>
          <View style={styles.circleBtnPlaceholder} />
        </View>

        {loading ? (
          <View style={styles.centerState}>
            <ActivityIndicator color={Colors.accent} />
            <Text style={styles.centerText}>Loading report...</Text>
          </View>
        ) : error ? (
          <View style={styles.centerState}>
            <Text style={styles.errorText}>{error}</Text>
          </View>
        ) : session ? (
          <>
            <View style={styles.hero}>
              <View>
                <Text style={styles.eyebrow}>{session.status.toUpperCase()}</Text>
                <Text style={styles.headline}>{session.role_title}</Text>
                <Text style={styles.subheadline}>{session.industry} | {session.date} | {session.duration} min</Text>
                {session.score_band ? (
                  <Text style={styles.bandText}>{session.score_band.label}: {session.score_band.description}</Text>
                ) : null}
                <Text style={styles.signalText}>
                  {hasUsableSignal
                    ? `${scorableAnswerCount || session.feedback.scorable_answers || session.answers.length} scored ${scorableAnswerCount === 1 ? 'answer' : 'answers'}`
                    : 'No usable transcript captured'}
                </Text>
              </View>
              <View style={[styles.scoreBadge, { borderColor: scoreColor(avgScore) }]}>
                <Text style={styles.scoreValue}>{avgScore}</Text>
                <Text style={styles.scoreLabel}>score</Text>
              </View>
            </View>

            <Pressable
              onPress={async () => {
                if (!hasPremiumAccess) {
                  router.push('/(modals)/paywall' as any);
                  return;
                }
                try {
                  setExportError('');
                  await exportSessionReportPdf(session, avgScore);
                } catch (err: any) {
                  setExportError(err?.message || 'Unable to export this report right now.');
                }
              }}
              style={styles.exportBtn}
            >
              <Feather name="download" size={16} color="#FFFFFF" />
              <Text style={styles.exportText}>{hasPremiumAccess ? 'Export PDF report' : 'Export PDF report Pro'}</Text>
            </Pressable>
            {exportError ? <Text style={styles.exportError}>{exportError}</Text> : null}

            <View style={styles.card}>
              <Text style={styles.sectionTitle}>Coach Summary</Text>
              <Text style={styles.summaryText}>
                {reportSummary}
              </Text>
              {hasUsableSignal ? (
                <View style={styles.focusGrid}>
                  <View style={styles.focusPill}>
                    <Text style={styles.focusLabel}>Strongest</Text>
                    <Text style={styles.focusValue}>{session.feedback.strongest_metric || 'Measured after more answers'}</Text>
                  </View>
                  <View style={styles.focusPill}>
                    <Text style={styles.focusLabel}>Focus</Text>
                    <Text style={styles.focusValue}>{session.feedback.weakest_metric || 'Measured after more answers'}</Text>
                  </View>
                </View>
              ) : (
                <View style={styles.signalCard}>
                  <Feather name="mic-off" size={18} color="#FFB020" />
                  <View style={{ flex: 1 }}>
                    <Text style={styles.signalTitle}>Report needs clearer audio</Text>
                    <Text style={styles.signalBody}>Skill rankings are hidden until at least one answer has a usable transcript and score.</Text>
                  </View>
                </View>
              )}
            </View>

            {hasUsableSignal ? (
              <View style={styles.card}>
                <Text style={styles.sectionTitle}>Strengths and weak spots</Text>
                <Text style={styles.chartIntro}>
                  A simple view of where this interview was strong and what needs more practice.
                </Text>
                <StrengthBars data={session.analytics.radar_data || []} />
              </View>
            ) : null}

            <View style={styles.card}>
              <Text style={styles.sectionTitle}>Next Coaching Plan</Text>
              {coachingPlan.map((item, index) => (
                <Text key={`${item}-${index}`} style={styles.bullet}>- {item}</Text>
              ))}
            </View>

            {hasUsableSignal ? (
              <View style={styles.card}>
                <Text style={styles.sectionTitle}>What to improve</Text>
                {(session.feedback.growth_areas.length ? session.feedback.growth_areas : ['No weak area detected yet. Complete more answered questions to build a stronger report.']).map((item, index) => (
                  <Text key={`${item}-${index}`} style={styles.bullet}>- {item}</Text>
                ))}
              </View>
            ) : null}

            <View style={styles.card}>
              <Text style={styles.sectionTitle}>Answer Review</Text>
              {session.answers.map((answer) => {
                const missingEvidence = asTextList(answer.missing_evidence);
                const score = answerScore(answer);
                return (
                  <View key={answer.id || String(answer.question_id)} style={styles.answerCard}>
                    <View style={styles.answerHeader}>
                      <Text style={styles.answerTitle}>Question {answer.question_id}</Text>
                      <Text style={[styles.answerScore, { color: scoreColor(score) }]}>{score}</Text>
                    </View>
                    <Text style={styles.answerTranscript} numberOfLines={4}>{answer.transcript || 'No usable transcript captured for this answer.'}</Text>
                    {answer.feedback ? <Text style={styles.answerFeedback}>{answer.feedback}</Text> : null}
                    {answer.improved_answer ? (
                      <View style={styles.savedAnswerBox}>
                        <Text style={styles.savedAnswerLabel}>Stronger saved answer</Text>
                        <Text style={styles.savedAnswerText}>{answer.improved_answer}</Text>
                        {missingEvidence.length ? (
                          <Text style={styles.savedAnswerMeta}>Missing: {missingEvidence.join(', ')}</Text>
                        ) : null}
                      </View>
                    ) : null}
                  </View>
                );
              })}
            </View>
          </>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.bgPrimary },
  content: { paddingBottom: 40 },
  header: {
    height: 72,
    paddingHorizontal: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  circleBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255,255,255,0.06)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  circleBtnPlaceholder: { width: 44, height: 44 },
  headerTitle: { color: '#FFFFFF', fontSize: 18, fontWeight: '900' },
  centerState: { minHeight: 420, alignItems: 'center', justifyContent: 'center', padding: 24 },
  centerText: { color: Colors.textSecondary, marginTop: 12, fontWeight: '700' },
  errorText: { color: '#FFB4BE', fontSize: 16, lineHeight: 23, textAlign: 'center', fontWeight: '700' },
  hero: {
    marginHorizontal: 20,
    marginTop: 18,
    marginBottom: 16,
    padding: 22,
    borderRadius: 28,
    backgroundColor: Colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.07)',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
  },
  eyebrow: { color: Colors.accent, fontSize: 12, fontWeight: '900', letterSpacing: 1.4, marginBottom: 8 },
  headline: { color: '#FFFFFF', fontSize: 28, fontWeight: '900', lineHeight: 34, maxWidth: width - 160 },
  subheadline: { color: Colors.textSecondary, fontSize: 13, lineHeight: 19, fontWeight: '700', marginTop: 8, maxWidth: width - 160 },
  bandText: { color: '#FFFFFF', fontSize: 13, lineHeight: 19, fontWeight: '800', marginTop: 10, maxWidth: width - 160 },
  signalText: { color: Colors.textSecondary, fontSize: 12, lineHeight: 18, fontWeight: '800', marginTop: 8, maxWidth: width - 160 },
  scoreBadge: {
    width: 86,
    height: 86,
    borderRadius: 28,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255,255,255,0.03)',
  },
  scoreValue: { color: '#FFFFFF', fontSize: 30, fontWeight: '900' },
  scoreLabel: { color: Colors.textSecondary, fontSize: 10, fontWeight: '900', letterSpacing: 1.2, textTransform: 'uppercase' },
  exportBtn: {
    marginHorizontal: 20,
    marginBottom: 14,
    height: 50,
    borderRadius: 25,
    backgroundColor: Colors.accent,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 9,
  },
  exportText: { color: '#FFFFFF', fontSize: 14, fontWeight: '900' },
  exportError: {
    color: '#FFB4BE',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
    marginHorizontal: 24,
    marginTop: -6,
    marginBottom: 14,
    textAlign: 'center',
  },
  card: {
    marginHorizontal: 20,
    marginBottom: 14,
    padding: 20,
    borderRadius: 26,
    backgroundColor: Colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.07)',
  },
  sectionTitle: { color: '#FFFFFF', fontSize: 20, fontWeight: '900', marginBottom: 14 },
  chartIntro: {
    color: Colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
    marginTop: -6,
    marginBottom: 18,
  },
  summaryText: { color: Colors.textSecondary, fontSize: 15, lineHeight: 23, fontWeight: '700' },
  focusGrid: { flexDirection: 'row', gap: 10, marginTop: 16 },
  focusPill: {
    flex: 1,
    borderRadius: 18,
    padding: 14,
    backgroundColor: 'rgba(108, 99, 255, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.18)',
  },
  focusLabel: { color: Colors.textSecondary, fontSize: 10, fontWeight: '900', textTransform: 'uppercase', letterSpacing: 1 },
  focusValue: { color: '#FFFFFF', fontSize: 15, fontWeight: '900', marginTop: 6 },
  signalCard: {
    marginTop: 16,
    borderRadius: 18,
    padding: 14,
    backgroundColor: 'rgba(255, 176, 32, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(255, 176, 32, 0.22)',
    flexDirection: 'row',
    gap: 12,
    alignItems: 'flex-start',
  },
  signalTitle: { color: '#FFFFFF', fontSize: 14, fontWeight: '900', marginBottom: 4 },
  signalBody: { color: Colors.textSecondary, fontSize: 13, lineHeight: 19, fontWeight: '700' },
  bullet: { color: Colors.textSecondary, fontSize: 15, lineHeight: 23, fontWeight: '600', marginBottom: 8 },
  answerCard: {
    paddingVertical: 16,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.07)',
  },
  answerHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 },
  answerTitle: { color: '#FFFFFF', fontSize: 15, fontWeight: '900' },
  answerScore: { fontSize: 18, fontWeight: '900' },
  answerTranscript: { color: Colors.textSecondary, fontSize: 14, lineHeight: 21, fontWeight: '600' },
  answerFeedback: { color: '#FFFFFF', fontSize: 14, lineHeight: 21, fontWeight: '700', marginTop: 10 },
  savedAnswerBox: {
    marginTop: 12,
    borderRadius: 18,
    backgroundColor: 'rgba(108, 99, 255, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.2)',
    padding: 14,
  },
  savedAnswerLabel: { color: Colors.accent, fontSize: 11, fontWeight: '900', textTransform: 'uppercase', letterSpacing: 1.2, marginBottom: 8 },
  savedAnswerText: { color: '#FFFFFF', fontSize: 14, lineHeight: 21, fontWeight: '700' },
  savedAnswerMeta: { color: Colors.textSecondary, fontSize: 12, lineHeight: 18, marginTop: 8, fontWeight: '700' },
});
