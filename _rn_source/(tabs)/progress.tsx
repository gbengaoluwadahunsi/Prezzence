import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { router, useFocusEffect } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  View,
  Pressable,
  ActivityIndicator
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { StrengthBars } from '../../src/components/charts/StrengthBars';
import { useAuthStore } from '../../src/store/authStore';
import { getUserProgress, ProgressData } from '../../src/api/users';

import { useTranslation } from '../../src/hooks/useTranslation';

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  textSecondary: '#8A8A9A',
  card: '#1C1C2E',
  border: '#2A2A3E',
  cyan: '#2FD0B4',
  success: '#00D68F',
};

function StatCard({ label, value, unit }: { label: string, value: string | number, unit: string }) {
  return (
    <View style={styles.statCard}>
      <Text style={styles.statLabel} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.72}>
        {label}
      </Text>
      <View style={{ flexDirection: 'row', alignItems: 'baseline' }}>
        <Text style={styles.statValue}>{value}</Text>
        <Text style={styles.statUnit}>{unit}</Text>
      </View>
    </View>
  );
}

function DeltaPill({ value }: { value: number }) {
  const positive = value > 0;
  const neutral = value === 0;
  return (
    <View style={[
      styles.deltaPill,
      positive ? styles.deltaPositive : neutral ? styles.deltaNeutral : styles.deltaNegative
    ]}>
      <Feather
        name={positive ? 'trending-up' : neutral ? 'minus' : 'trending-down'}
        size={14}
        color={positive ? Colors.success : neutral ? Colors.textSecondary : '#FF5C7A'}
      />
      <Text style={[
        styles.deltaText,
        { color: positive ? Colors.success : neutral ? Colors.textSecondary : '#FF5C7A' }
      ]}>
        {positive ? '+' : ''}{value}
      </Text>
    </View>
  );
}

export default function ProgressDashboardScreen() {
  const { user } = useAuthStore();
  const { t, language } = useTranslation();
  const [loading, setLoading] = useState(true);
  const [progress, setProgress] = useState<ProgressData | null>(null);

  const fetchProgress = useCallback(async () => {
    try {
      if (user?.id) {
        const data = await getUserProgress(user.id, language);
        setProgress(data);
      }
    } catch (error) {
      console.error("Failed to load progress data", error);
    } finally {
      setLoading(false);
    }
  }, [user?.id, language]);

  useEffect(() => {
    void fetchProgress();
  }, [fetchProgress]);

  useFocusEffect(
    useCallback(() => {
      if (!progress) setLoading(true);
      void fetchProgress();
    }, [fetchProgress, progress])
  );

  if (loading) {
    return (
      <SafeAreaView style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={Colors.accent} />
      </SafeAreaView>
    );
  }

  const avgScore = progress?.stats?.avg_score ?? 0;
  const sessions = progress?.stats?.sessions ?? 0;
  const practiceHours = progress?.stats?.practice_hours ?? 0;
  const growth = progress?.growth ?? 0;
  const improvement = progress?.improvement;
  const readiness = progress?.readiness?.label || 'No Baseline';
  const trend = (progress?.session_trend || []).filter(Boolean);
  const plan = progress?.coaching_plan || [];
  const weaknesses = progress?.repeated_weaknesses || [];
  const drills = progress?.drills || [];
  const hasProgressSignal = sessions > 0 && avgScore > 0 && trend.length > 0;
  const focus = hasProgressSignal ? progress?.skill_focus : null;
  const strengthData = hasProgressSignal && progress?.radar_data ? progress.radar_data : [
    { label: t.progress.skills.clarity, value: 0 },
    { label: t.progress.skills.pacing, value: 0 },
    { label: t.progress.skills.impact, value: 0 },
    { label: t.progress.skills.confidence, value: 0 },
    { label: t.progress.skills.knowledge, value: 0 },
  ];
  const performanceTitle =
    avgScore >= 85 ? 'Interview Ready' :
    avgScore >= 70 ? 'Building Confidence' :
    avgScore >= 55 ? 'Building Momentum' :
    avgScore > 0 ? 'Foundation Built' :
    'Ready to Practice';
  const growthText =
    sessions === 0
      ? 'Complete your first interview to build your baseline.'
      : growth > 0
        ? t.progress.growthSub.replace('{0}', growth.toString())
        : growth < 0
          ? `Your interview baseline moved ${Math.abs(growth)}% this session.`
          : 'First session complete. Your interview baseline is ready.';

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <Text style={styles.headerTitle}>{t.progress.performance}</Text>
          <Pressable
            style={styles.circleBtn}
            onPress={() => router.push('/settings' as any)}
            accessibilityRole="button"
            accessibilityLabel="Open settings"
          >
            <Feather name="settings" size={20} color="#FFFFFF" />
          </Pressable>
        </View>

        {/* Global Summary */}
        <View style={styles.summarySection}>
          <Text style={styles.greetingHeader}>{performanceTitle}</Text>
          <Text style={styles.subheadline}>{growthText}</Text>
        </View>

        <View style={styles.statsRow}>
          <StatCard label={t.progress.avgScore} value={avgScore} unit="%" />
          <StatCard label={t.progress.sessions} value={sessions} unit="" />
          <StatCard label={t.progress.practice} value={practiceHours} unit="hrs" />
        </View>

        {hasProgressSignal ? (
          <View style={styles.improvementCard}>
            <View style={styles.improvementHeader}>
              <View>
                <Text style={styles.cardTitle}>Improvement</Text>
                <Text style={styles.mutedText}>Interview readiness: {readiness}</Text>
              </View>
              <DeltaPill value={improvement?.from_first ?? 0} />
            </View>
            <View style={styles.compareRow}>
              <View style={styles.compareBox}>
                <Text style={styles.compareLabel}>First</Text>
                <Text style={styles.compareValue}>{improvement?.first_score ?? 0}%</Text>
              </View>
              <Feather name="arrow-right" size={18} color={Colors.textSecondary} />
              <View style={styles.compareBox}>
                <Text style={styles.compareLabel}>Latest</Text>
                <Text style={styles.compareValue}>{improvement?.latest_score ?? avgScore}%</Text>
              </View>
              <View style={styles.compareBox}>
                <Text style={styles.compareLabel}>Best</Text>
                <Text style={styles.compareValue}>{improvement?.best_score ?? avgScore}%</Text>
              </View>
            </View>
            {focus ? (
              <Text style={styles.improvementCopy}>
                {focus.strongest_score >= 70 ? 'Strongest' : 'Best current signal'}: {focus.strongest} at {focus.strongest_score}%. Focus next on {focus.weakest} at {focus.weakest_score}%.
              </Text>
            ) : null}
            <View style={styles.trendBars}>
              {trend.slice(-8).map((item, index) => (
                <View key={item.id || `${item.date}-${index}`} style={styles.trendItem}>
                  <View style={[styles.trendBar, { height: Math.max(8, item.score) }]} />
                  <Text style={styles.trendScore}>{item.score}</Text>
                </View>
              ))}
            </View>
          </View>
        ) : (
          <View style={styles.improvementCard}>
            <Text style={styles.cardTitle}>Progress starts after your first scored answer</Text>
            <Text style={styles.improvementCopy}>
              Complete an interview with a clear transcript. Once at least one answer is scored, this page will show real improvement, strengths, and coaching priorities.
            </Text>
          </View>
        )}

        {hasProgressSignal ? (
          <View style={styles.chartCard}>
            <Text style={styles.cardTitle}>Strengths and weak spots</Text>
            <Text style={styles.chartIntro}>
              These bars show where your answers are strongest and what to practice next.
            </Text>
            <StrengthBars data={strengthData} />
          </View>
        ) : null}

        {hasProgressSignal && plan.length ? (
          <View style={styles.planCard}>
            <Text style={styles.cardTitle}>Next steps</Text>
            {plan.map((item, index) => (
              <View key={`${item}-${index}`} style={styles.planRow}>
                <View style={styles.planNumber}>
                  <Text style={styles.planNumberText}>{index + 1}</Text>
                </View>
                <Text style={styles.planText}>{item}</Text>
              </View>
            ))}
          </View>
        ) : null}

        {hasProgressSignal && drills.length ? (
          <View style={styles.planCard}>
            <Text style={styles.cardTitle}>Focused practice</Text>
            {drills.map((item, index) => (
              <View key={`${item.title}-${index}`} style={styles.drillCard}>
                <Text style={styles.drillTitle}>{item.title}</Text>
                <Text style={styles.drillText}>{item.description}</Text>
              </View>
            ))}
          </View>
        ) : null}

        {hasProgressSignal && weaknesses.length ? (
          <View style={styles.planCard}>
            <Text style={styles.cardTitle}>Repeated weak spots</Text>
            {weaknesses.slice(0, 4).map((item, index) => (
              <View key={`${item}-${index}`} style={styles.weaknessRow}>
                <Feather name="alert-circle" size={16} color="#FFB020" />
                <Text style={styles.planText}>{item}</Text>
              </View>
            ))}
          </View>
        ) : null}

        {/* Insights */}
        <View style={styles.insightCard}>
          <LinearGradient
            colors={['rgba(108, 99, 255, 0.1)', 'transparent']}
            style={styles.insightGradient}
          />
          <View style={styles.insightIcon}>
            <MaterialCommunityIcons name="lightning-bolt" size={24} color={Colors.accent} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.insightTitle}>Coaching tip</Text>
            <Text style={styles.insightText}>
              {progress?.coaching_tip || 'Complete one interview with clear audio to unlock personalized coaching.'}
            </Text>
          </View>
        </View>

        {/* History Preview */}
        <View style={styles.historyHeader}>
          <Text style={styles.sectionTitle}>{t.progress.recentSessions}</Text>
          <Pressable
            onPress={() => router.push('/sessions' as any)}
            hitSlop={10}
          >
            <Text style={styles.viewAll}>{t.progress.viewAll}</Text>
          </Pressable>
        </View>

        {(progress?.recent_sessions || []).filter(Boolean).map((item, index) => (
          <Pressable
            key={item.id || `${item.role}-${item.date}-${index}`}
            onPress={() => item.id && router.push(`/sessions/${item.id}` as any)}
            style={({ pressed }) => [styles.historyItem, pressed && { opacity: 0.84 }]}
          >
            <View style={[styles.historyBadge, { backgroundColor: index === 0 ? Colors.success + '20' : '#FFFFFF10' }]}>
              <Text style={[styles.historyScore, { color: index === 0 ? Colors.success : '#FFFFFF' }]}>
                {item.score}
              </Text>
            </View>
            <View style={{ flex: 1, marginLeft: 16 }}>
              <Text style={styles.historyRole}>{item.role}</Text>
              <Text style={styles.historyDate}>{item.date}</Text>
            </View>
            <Feather name="chevron-right" size={20} color={Colors.textSecondary} />
          </Pressable>
        ))}

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bgPrimary,
  },
  scrollContent: {
    paddingBottom: 60,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    height: 80,
  },
  circleBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '800',
  },
  summarySection: {
    paddingHorizontal: 24,
    marginTop: 32,
    marginBottom: 40,
  },
  greetingHeader: {
    color: '#FFFFFF',
    fontSize: 42,
    fontWeight: '900',
    letterSpacing: -2,
    lineHeight: 48,
  },
  subheadline: {
    color: Colors.textSecondary,
    fontSize: 17,
    marginTop: 10,
    opacity: 0.8,
  },
  statsRow: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    justifyContent: 'space-between',
    marginBottom: 40,
    gap: 12,
  },
  statCard: {
    flex: 1,
    backgroundColor: Colors.card,
    borderRadius: 28,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  statLabel: {
    color: Colors.textSecondary,
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1.5,
    marginBottom: 16,
    textTransform: 'uppercase',
  },
  statValue: {
    color: '#FFFFFF',
    fontSize: 28,
    fontWeight: '900',
    letterSpacing: -1,
  },
  statUnit: {
    color: Colors.textSecondary,
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 4,
    marginBottom: 2,
  },
  chartCard: {
    backgroundColor: Colors.card,
    marginHorizontal: 20,
    borderRadius: 36,
    padding: 24,
    marginBottom: 32,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  chartIntro: {
    color: Colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
    marginTop: -8,
    marginBottom: 20,
  },
  improvementCard: {
    backgroundColor: Colors.card,
    marginHorizontal: 20,
    borderRadius: 28,
    padding: 22,
    marginBottom: 28,
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.22)',
  },
  improvementHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 14,
    marginBottom: 18,
  },
  mutedText: {
    color: Colors.textSecondary,
    fontSize: 13,
    fontWeight: '700',
    marginTop: 4,
  },
  deltaPill: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 7,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  deltaPositive: {
    backgroundColor: 'rgba(0, 214, 143, 0.12)',
  },
  deltaNeutral: {
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
  },
  deltaNegative: {
    backgroundColor: 'rgba(255, 92, 122, 0.12)',
  },
  deltaText: {
    fontSize: 13,
    fontWeight: '900',
  },
  compareRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: 16,
  },
  compareBox: {
    flex: 1,
    backgroundColor: 'rgba(255, 255, 255, 0.04)',
    borderRadius: 18,
    padding: 12,
  },
  compareLabel: {
    color: Colors.textSecondary,
    fontSize: 10,
    fontWeight: '900',
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 6,
  },
  compareValue: {
    color: '#FFFFFF',
    fontSize: 22,
    fontWeight: '900',
  },
  improvementCopy: {
    color: '#FFFFFF',
    fontSize: 14,
    lineHeight: 20,
    opacity: 0.86,
    marginBottom: 18,
  },
  trendBars: {
    height: 118,
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 10,
    paddingTop: 10,
  },
  trendItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: 6,
  },
  trendBar: {
    width: '100%',
    maxWidth: 28,
    borderRadius: 999,
    backgroundColor: Colors.accent,
  },
  trendScore: {
    color: Colors.textSecondary,
    fontSize: 10,
    fontWeight: '800',
  },
  planCard: {
    backgroundColor: Colors.card,
    marginHorizontal: 20,
    borderRadius: 28,
    padding: 22,
    marginBottom: 28,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    gap: 14,
  },
  planRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
  },
  planNumber: {
    width: 24,
    height: 24,
    borderRadius: 8,
    backgroundColor: 'rgba(108, 99, 255, 0.18)',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 1,
  },
  planNumberText: {
    color: Colors.accent,
    fontSize: 12,
    fontWeight: '900',
  },
  planText: {
    color: '#FFFFFF',
    fontSize: 14,
    lineHeight: 20,
    flex: 1,
    opacity: 0.88,
  },
  weaknessRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 10,
  },
  drillCard: {
    borderRadius: 18,
    backgroundColor: 'rgba(108, 99, 255, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.18)',
    padding: 14,
  },
  drillTitle: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '900',
    marginBottom: 5,
  },
  drillText: {
    color: Colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
  },
  cardTitle: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '900',
    marginBottom: 16,
    letterSpacing: -0.5,
  },
  insightCard: {
    backgroundColor: Colors.card,
    marginHorizontal: 20,
    borderRadius: 28,
    padding: 24,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    overflow: 'hidden',
    marginBottom: 40,
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.2)',
  },
  insightGradient: {
    ...StyleSheet.absoluteFillObject,
  },
  insightIcon: {
    width: 56,
    height: 56,
    borderRadius: 16,
    backgroundColor: 'rgba(108, 99, 255, 0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  insightTitle: {
    color: Colors.accent,
    fontSize: 15,
    fontWeight: '900',
    marginBottom: 4,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  insightText: {
    color: '#FFFFFF',
    fontSize: 15,
    lineHeight: 22,
    opacity: 0.85,
  },
  historyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    paddingHorizontal: 24,
    marginBottom: 24,
  },
  sectionTitle: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: '900',
    letterSpacing: -0.5,
  },
  viewAll: {
    color: Colors.accent,
    fontSize: 15,
    fontWeight: '800',
  },
  historyItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingVertical: 20,
    marginHorizontal: 20,
    backgroundColor: Colors.card,
    borderRadius: 24,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.03)',
  },
  historyBadge: {
    width: 48,
    height: 48,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  historyScore: {
    fontSize: 18,
    fontWeight: '900',
  },
  historyRole: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '800',
    marginBottom: 4,
  },
  historyDate: {
    color: Colors.textSecondary,
    fontSize: 14,
    fontWeight: '500',
  },
});
