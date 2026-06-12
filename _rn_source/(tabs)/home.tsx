import { Feather } from '@expo/vector-icons';
import MaterialCommunityIcons from '@expo/vector-icons/MaterialCommunityIcons';
import { LinearGradient } from 'expo-linear-gradient';
import { router, useFocusEffect } from 'expo-router';
import React from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  View,
  Pressable,
  Dimensions,
  Image
} from 'react-native';
import { useAuthStore } from '../../src/store/authStore';
import { useSessionStore } from '../../src/store/sessionStore';
import { useTranslation } from '../../src/hooks/useTranslation';
import { SafeAreaView } from 'react-native-safe-area-context';
import Animated, { FadeInDown } from 'react-native-reanimated';

const { width } = Dimensions.get('window');

import { colors, spacing, typography, radii } from '../../src/theme';

import { getNotifications, getUserProgress, ProgressData } from '../../src/api/users';
import { ActivityIndicator } from 'react-native';
import { BrandWordmark } from '../../src/components/BrandWordmark';
import { getLocalPersonaImage } from '../../src/utils/personaAssets';
import { getPersonasForInterview } from '../../src/data/personas';

export default function HomeScreen() {
  const { user } = useAuthStore();
  const { t } = useTranslation();
  const {
    selectedPersonas,
    roleTitle,
    industry,
    interviewerMode,
    language,
    activeSessionId,
    questions,
    answers,
    currentQuestionIndex,
    resetSession,
  } = useSessionStore();
  const [progress, setProgress] = React.useState<ProgressData | null>(null);
  const [unreadCount, setUnreadCount] = React.useState(0);
  const [loading, setLoading] = React.useState(true);

  const firstName = user?.user_metadata?.full_name?.split(' ')[0] || user?.email?.split('@')[0] || '';

  useFocusEffect(
    React.useCallback(() => {
      if (user?.id) {
        fetchData();
      }
    }, [user?.id, language])
  );

  const fetchData = async () => {
    try {
      setLoading(true);
      const data = await getUserProgress(user!.id, language);
      setProgress(data);
      setLoading(false);
      getNotifications()
        .then((notifications) => {
          setUnreadCount(notifications.filter((item: any) => !item.is_read).length);
        })
        .catch(() => undefined);
    } catch (err) {
      console.error('[HomeScreen] Failed to fetch progress:', err);
      setLoading(false);
    }
  };

  const avgScore = progress?.stats?.avg_score || 0;
  const coachingTip = progress?.coaching_tip || t.home.readinessAdvice;
  const sessionCount = progress?.stats?.sessions ?? 0;
  const sessionLabel = sessionCount === 1 ? 'session' : 'sessions';
  const readinessLabel = progress?.readiness?.label || 'No Baseline';
  const improvement = progress?.improvement?.from_first ?? 0;
  const hasReadinessSignal = sessionCount > 0 && avgScore > 0;
  const nextFocus = hasReadinessSignal ? progress?.skill_focus?.weakest : null;
  const hasIncompleteSession = Boolean(activeSessionId && questions.length > 0 && answers.length < questions.length);
  const displayPersonas = hasIncompleteSession
    ? selectedPersonas.length
      ? selectedPersonas
      : getPersonasForInterview(interviewerMode || 'single', 'neutral', industry || 'General Business')
    : [];
  const milestoneTargets = [1, 3, 5, 10, 20, 50];
  const nextMilestoneTarget = milestoneTargets.find((target) => sessionCount < target);
  const milestoneDesc = nextMilestoneTarget
    ? sessionCount === 0
      ? 'Complete your first practice session to start your streak.'
      : `${nextMilestoneTarget - sessionCount} more ${nextMilestoneTarget - sessionCount === 1 ? 'session' : 'sessions'} to reach ${nextMilestoneTarget} completed sessions.`
    : 'Keep your streak alive with another practice session.';

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* Modernized Header */}
        <View style={styles.header}>
          <BrandWordmark size={30} textSize={15} />
          <Pressable
            style={styles.circleBtn}
            onPress={() => router.push('/profile/notifications' as any)}
            accessibilityRole="button"
            accessibilityLabel="Open notifications"
          >
            <Feather name="bell" size={22} color={colors.accent} />
            {unreadCount > 0 ? (
              <View style={styles.notificationBadge}>
                <Text style={styles.notificationBadgeText}>
                  {unreadCount > 9 ? '9+' : unreadCount}
                </Text>
              </View>
            ) : null}
          </Pressable>
        </View>

        {/* Dynamic Hero Section */}
        <View style={styles.heroSection}>
          <Text style={styles.greeting}>
            {t.home.welcome.replace('{name}', firstName || '!')}
          </Text>
          <Text style={styles.subGreeting}>
            {sessionCount > 0
              ? `You've completed ${sessionCount} ${sessionLabel}. Ready for the next one?`
              : t.onboarding.languageSubheadline}
          </Text>

          {/* Current Panel Mini Preview */}
          {hasIncompleteSession ? (
            <View style={styles.panelPreview}>
              {displayPersonas.map((p, i) => (
                <View key={p.id} style={[styles.miniAvatar, { left: i * 28, zIndex: 3 - i }]}>
                  <Image source={getLocalPersonaImage(p, language)} style={styles.miniAvatarImage} />
                </View>
              ))}
              <Text style={[styles.panelLabel, { marginLeft: displayPersonas.length * 28 + 12 }]}>
                {displayPersonas.length === 1 ? '1 interviewer ready' : `${displayPersonas.length} interviewers ready`}
              </Text>
            </View>
          ) : (
            <Text style={styles.panelLabel}>0 interviewers ready</Text>
          )}
        </View>

        {hasIncompleteSession ? (
          <View style={styles.resumeCard}>
            <View style={styles.resumeHeader}>
              <View style={[styles.iconBox, { backgroundColor: 'rgba(108, 99, 255, 0.14)' }]}>
                <Feather name="play-circle" size={22} color={colors.accent} />
              </View>
              <View style={styles.resumeCopy}>
                <Text style={styles.resumeTitle}>Continue unfinished interview?</Text>
                <Text style={styles.resumeMeta}>
                  Question {Math.min(currentQuestionIndex + 1, questions.length)} of {questions.length}
                </Text>
              </View>
            </View>
            <View style={styles.resumeActions}>
              <Pressable
                onPress={() => router.push('/interview/speaking' as any)}
                style={styles.resumePrimary}
              >
                <Text style={styles.resumePrimaryText}>Continue</Text>
              </Pressable>
              <Pressable
                onPress={() => {
                  resetSession();
                  router.push('/onboarding/role' as any);
                }}
                style={styles.resumeSecondary}
              >
                <Text style={styles.resumeSecondaryText}>Start over</Text>
              </Pressable>
            </View>
          </View>
        ) : null}

        {/* Readiness summary */}
        <View style={styles.readinessCard}>
          <LinearGradient
            colors={['rgba(108, 99, 255, 0.1)', 'transparent']}
            style={styles.readinessGradient}
          />
          <View style={styles.readinessHeader}>
            <View>
              <Text style={styles.cardLabel}>{t.home.currentReadiness}</Text>
              <Text style={styles.readinessBand}>{readinessLabel}</Text>
            </View>
            {loading ? (
              <ActivityIndicator size="small" color={colors.accent} />
            ) : (
              <Text style={styles.readinessValue}>{avgScore}%</Text>
            )}
          </View>
          <View style={styles.readinessBar}>
            <View style={[styles.readinessFill, { width: `${avgScore}%` }]} />
          </View>
          <View style={styles.readinessMetaRow}>
            <Text style={styles.readinessMeta}>
              {hasReadinessSignal
                ? improvement > 0
                  ? `+${improvement} since first session`
                  : improvement < 0
                    ? `${improvement} since first session`
                    : 'Baseline captured'
                : 'Baseline in progress'}
            </Text>
            {nextFocus ? <Text style={styles.readinessMeta}>Next: {nextFocus}</Text> : null}
          </View>
          <Text style={styles.readinessAdvice}>{coachingTip}</Text>
        </View>

        <View style={styles.ctaSection}>
          <Pressable
            onPress={() => router.push('/onboarding/role' as any)}
            style={({ pressed }) => [
              styles.primaryButton,
              pressed && { opacity: 0.9 }
            ]}
          >
            <Text style={styles.buttonText}>{t.home.startNewSession}</Text>
            <MaterialCommunityIcons name="chevron-right" size={24} color="#FFFFFF" />
          </Pressable>
        </View>

        {/* Simplified Action Cards */}
        <View style={styles.actionGrid}>
          <Pressable style={styles.entryCard} onPress={() => router.push('/onboarding/role' as any)}>
            <View style={[styles.iconBox, { backgroundColor: 'rgba(0, 214, 143, 0.1)' }]}>
              <Feather name="play-circle" size={24} color="#00D68F" />
            </View>
            <Text style={styles.entryTitle}>New interview</Text>
            <Text style={styles.entryMeta}>{roleTitle || 'General'}</Text>
          </Pressable>

          <Pressable style={styles.entryCard} onPress={() => router.push('/(tabs)/practice' as any)}>
            <View style={[styles.iconBox, { backgroundColor: 'rgba(108, 99, 255, 0.1)' }]}>
              <Feather name="target" size={24} color={colors.accent} />
            </View>
            <Text style={styles.entryTitle}>{t.home.switchTrack}</Text>
            <Text style={styles.entryMeta}>{t.home.modesAvailable}</Text>
          </Pressable>
        </View>

        {/* Milestone Indicator */}
        <View style={styles.milestoneCard}>
          <MaterialCommunityIcons name="trophy-outline" size={24} color="#FFB020" />
          <View style={styles.milestoneInfo}>
            <Text style={styles.milestoneTitle}>{t.home.nextMilestone}</Text>
            <Text style={styles.milestoneDesc}>{milestoneDesc}</Text>
          </View>
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  scrollContent: {
    paddingBottom: 96,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    height: 80,
  },
  logoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  logoText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
    letterSpacing: 2,
  },
  betaBadge: {
    backgroundColor: 'rgba(108, 99, 255, 0.15)',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
  },
  betaText: {
    color: colors.accent,
    fontSize: 9,
    fontWeight: '900',
  },
  circleBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(28, 28, 46, 0.6)',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  notificationBadge: {
    position: 'absolute',
    top: 7,
    right: 7,
    minWidth: 16,
    height: 16,
    borderRadius: 8,
    paddingHorizontal: 4,
    backgroundColor: '#FF4757',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.background,
  },
  notificationBadgeText: {
    color: '#FFFFFF',
    fontSize: 9,
    fontWeight: '900',
  },
  heroSection: {
    paddingHorizontal: 24,
    marginTop: 20,
    marginBottom: 32,
  },
  greeting: {
    color: '#FFFFFF',
    fontSize: 40,
    fontWeight: '900',
    lineHeight: 48,
    letterSpacing: -1,
  },
  subGreeting: {
    color: colors.textMuted,
    fontSize: 16,
    marginTop: 8,
  },
  panelPreview: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 20,
    height: 40,
  },
  miniAvatar: {
    position: 'absolute',
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 2,
    borderColor: '#0A0A0F',
    overflow: 'hidden',
  },
  miniAvatarImage: {
    width: '100%',
    height: '100%',
  },
  miniAvatarPlaceholder: {
    width: '100%',
    height: '100%',
    alignItems: 'center',
    justifyContent: 'center',
  },
  miniAvatarText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '800',
  },
  panelLabel: {
    color: colors.accent,
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 0.2,
  },
  resumeCard: {
    marginHorizontal: 20,
    marginBottom: 22,
    borderRadius: 26,
    padding: 18,
    backgroundColor: 'rgba(28, 28, 46, 0.82)',
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.34)',
  },
  resumeHeader: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  resumeCopy: {
    flex: 1,
  },
  resumeTitle: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '900',
  },
  resumeMeta: {
    color: colors.textMuted,
    fontSize: 13,
    fontWeight: '700',
    marginTop: 4,
  },
  resumeActions: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 16,
  },
  resumePrimary: {
    flex: 1,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  resumePrimaryText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '900',
  },
  resumeSecondary: {
    flex: 1,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  resumeSecondaryText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '800',
  },
  readinessCard: {
    backgroundColor: 'rgba(28, 28, 46, 0.4)',
    marginHorizontal: 20,
    borderRadius: 32,
    padding: 24,
    marginBottom: 24,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  readinessGradient: {
    ...StyleSheet.absoluteFillObject,
  },
  readinessHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    marginBottom: 16,
  },
  cardLabel: {
    color: colors.textMuted,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1.5,
  },
  readinessValue: {
    color: '#FFFFFF',
    fontSize: 32,
    fontWeight: '900',
  },
  readinessBand: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '900',
    marginTop: 6,
  },
  readinessBar: {
    height: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 4,
    marginBottom: 16,
  },
  readinessFill: {
    height: '100%',
    backgroundColor: colors.accent,
    borderRadius: 4,
  },
  readinessMetaRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
    marginBottom: 12,
  },
  readinessMeta: {
    color: colors.accent,
    fontSize: 12,
    fontWeight: '800',
    flexShrink: 1,
  },
  readinessAdvice: {
    color: colors.textMuted,
    fontSize: 13,
    lineHeight: 20,
  },
  ctaSection: {
    paddingHorizontal: 24,
    marginBottom: 28,
  },
  actionGrid: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    gap: 16,
    marginBottom: 24,
  },
  entryCard: {
    flex: 1,
    backgroundColor: colors.card,
    borderRadius: 24,
    padding: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.03)',
  },
  iconBox: {
    width: 48,
    height: 48,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  entryTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
    marginBottom: 4,
  },
  entryMeta: {
    color: colors.textMuted,
    fontSize: 12,
  },
  milestoneCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 176, 32, 0.05)',
    marginHorizontal: 20,
    padding: 20,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: 'rgba(255, 176, 32, 0.1)',
    gap: 16,
  },
  milestoneInfo: {
    flex: 1,
  },
  milestoneTitle: {
    color: '#FFB020',
    fontSize: 15,
    fontWeight: '800',
    marginBottom: 2,
  },
  milestoneDesc: {
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 13,
  },
  primaryButton: {
    backgroundColor: colors.accent,
    height: 68,
    borderRadius: 34,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
    shadowColor: colors.accent,
    shadowOpacity: 0.4,
    shadowRadius: 20,
    elevation: 8,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '800',
  },
});
