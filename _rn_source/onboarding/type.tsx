import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import { Pressable, StyleSheet, Text, View, useWindowDimensions, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useSessionStore } from '../../src/store/sessionStore';
import { useSettingsStore } from '../../src/store/settingsStore';
import { useTranslation } from '../../src/hooks/useTranslation';
import { colors, spacing, typography, radii } from '../../src/theme';
import type { InterviewTrack } from '../../src/types';

interface TrackCardProps {
  title: string;
  subtitle: string;
  icon: keyof typeof Feather.glyphMap;
  accent: string;
  selected: boolean;
  onPress?: () => void;
  disabled?: boolean;
}

function TrackCard({ title, subtitle, icon, accent, selected, onPress, disabled }: TrackCardProps) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={[
        styles.card,
        selected ? styles.cardSelected : null,
        disabled ? styles.cardDisabled : null,
        { opacity: disabled ? 0.6 : 1 }
      ]}
    >
      <View style={[styles.iconContainer, { backgroundColor: selected ? colors.accentGlass : `${accent}20` }]}>
        <Feather name={icon} size={20} color={selected ? colors.accent : accent} />
      </View>

      {selected && (
        <View style={styles.checkBadge}>
          <Feather name="check" size={12} color={colors.text} />
        </View>
      )}

      <Text style={styles.cardTitle}>{title}</Text>
      <Text style={styles.cardSubtitle} numberOfLines={2}>{subtitle}</Text>
    </Pressable>
  );
}

export default function TypeScreen() {
  const selected = useSessionStore((state) => state.interviewType);
  const setInterviewType = useSessionStore((state) => state.setInterviewType);
  const { width } = useWindowDimensions();
  const { t } = useTranslation();
  const onboardingCompleted = useSettingsStore((state) => state.onboardingCompleted);
  const setOnboardingStep = useSettingsStore((state) => state.setOnboardingStep);

  React.useEffect(() => {
    setOnboardingStep('/onboarding/type');
  }, []);
  const cardWidth = (width - 48 - 16) / 2;

  const getIcon = (id: InterviewTrack['id']): keyof typeof Feather.glyphMap => {
    switch (id) {
      case 'job': return 'briefcase';
      case 'promotion': return 'trending-up';
      case 'pitch': return 'pie-chart';
      case 'leadership': return 'users';
      case 'behavioral': return 'message-circle';
      case 'technical': return 'cpu';
      default: return 'star';
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.headerNav}>
        <Pressable
          onPress={() => router.replace('/')}
          style={({ pressed }) => [
            styles.backButton,
            pressed && { opacity: 0.7 }
          ]}
        >
          <Feather name="arrow-left" size={24} color={colors.text} />
        </Pressable>
        <View style={styles.headerCenter}>
          <Text style={styles.progressText}>
            {onboardingCompleted ? 'NEW INTERVIEW' : `${t.common.step || 'STEP'} 1 / 2`}
          </Text>
        </View>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.headerText}>
          <Text style={styles.title}>{t.onboarding.typeHeadline}</Text>
          <Text style={styles.subtitle}>{t.onboarding.typeSubheadline}</Text>
        </View>

        <View style={styles.grid}>
          {t.tracks.map((type: InterviewTrack) => (
            <View key={type.id} style={[styles.gridItem, { width: cardWidth }]}>
              <TrackCard
                title={type.title}
                subtitle={type.subtitle}
                icon={getIcon(type.id)}
                accent={type.accent}
                selected={selected === type.id}
                onPress={() => setInterviewType(type.id)}
              />
            </View>
          ))}
          <View style={[styles.gridItem, { width: cardWidth }]}>
            <TrackCard
              title="More options"
              subtitle="Coming later"
              icon="star"
              accent={colors.textMuted}
              selected={false}
              disabled
            />
          </View>
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <Pressable
          onPress={() => router.push('/onboarding/role')}
          disabled={!selected}
          style={({ pressed }) => [
            styles.continueButton,
            !selected && styles.continueButtonDisabled,
            pressed && { opacity: 0.8 }
          ]}
        >
          <Text
            style={styles.continueText}
            numberOfLines={1}
            adjustsFontSizeToFit
          >
            Continue
          </Text>
          <Feather name="chevron-right" size={20} color={colors.text} style={styles.continueIcon} />
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  headerNav: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    height: 60,
  },
  headerCenter: {
    position: 'absolute',
    left: 0,
    right: 0,
    alignItems: 'center',
    zIndex: -1,
  },
  headerSpacer: {
    width: 40,
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  progressText: {
    color: colors.textMuted,
    fontSize: 14,
    fontWeight: '600',
  },
  scrollContent: {
    paddingHorizontal: 24,
    paddingBottom: 160,
  },
  headerText: {
    marginTop: 12,
    marginBottom: 20,
  },
  title: {
    color: colors.text,
    fontSize: 24,
    fontWeight: '800',
    lineHeight: 32,
  },
  subtitle: {
    color: colors.textMuted,
    fontSize: 16,
    marginTop: 6,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 16,
  },
  gridItem: {
    flexGrow: 0,
  },
  card: {
    backgroundColor: colors.card,
    borderRadius: 28,
    padding: 16,
    height: 164,
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  cardSelected: {
    backgroundColor: colors.cardElevated,
    borderColor: colors.accent,
    shadowColor: colors.accent,
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 8,
  },
  cardDisabled: {
    borderStyle: 'dashed',
    borderColor: colors.border,
    backgroundColor: 'transparent',
  },
  iconContainer: {
    width: 44,
    height: 44,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkBadge: {
    position: 'absolute',
    top: 16,
    right: 16,
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardTitle: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '700',
    marginTop: 14,
  },
  cardSubtitle: {
    color: colors.textMuted,
    fontSize: 13,
    lineHeight: 18,
    marginTop: 4,
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    paddingHorizontal: 24,
    paddingBottom: 34,
    backgroundColor: 'transparent',
  },
  continueButton: {
    backgroundColor: colors.accent,
    height: 64,
    borderRadius: 32,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 20,
    shadowColor: colors.accent,
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 8,
  },
  continueButtonDisabled: {
    opacity: 0.5,
  },
  continueText: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '700',
  },
  continueIcon: {
    marginLeft: 8,
  },
});
