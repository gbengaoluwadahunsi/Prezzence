import { router } from 'expo-router';
import { Feather } from '@expo/vector-icons';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View, Switch, Modal, FlatList } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useSessionStore } from '../../src/store/sessionStore';
import { useSettingsStore } from '../../src/store/settingsStore';
import { useTranslation } from '../../src/hooks/useTranslation';
import { colors, spacing, typography, radii } from '../../src/theme';
import { getPersonasForInterview } from '../../src/data/personas';
import { BETA_UNLOCK_ALL_FEATURES } from '../../src/config/featureFlags';
import type { InterviewerMode, InterviewerPersonality } from '../../src/utils/interviewerStyle';

const seniorityLevels = ['Junior', 'Mid', 'Senior', 'Executive'];
type UserGender = 'female' | 'male';

export default function RoleScreen() {
  const {
    roleTitle: currentRole,
    seniority: currentSeniority,
    industry: currentIndustry,
    companyName: currentCompanyName,
    companyWebsite: currentCompanyWebsite,
    companyContext: currentCompanyContext,
    enableWebResearch,
    setEnableWebResearch,
    userGender,
    setUserGender,
    setRoleDetails,
    setIndustry: setStoreIndustry,
    setCompanyDetails,
    isTechnical,
    setIsTechnical,
    isPremium,
    difficulty,
    setDifficulty,
    interviewerMode,
    setInterviewerMode,
    interviewerPersonality,
    setInterviewerPersonality,
    selectedPersonas,
    setSelectedPersonas
  } = useSessionStore();
  const { t, language } = useTranslation();
  const { onboardingCompleted, setOnboardingStep } = useSettingsStore();
  const hasPremiumAccess = BETA_UNLOCK_ALL_FEATURES || isPremium;

  React.useEffect(() => {
    setOnboardingStep('/onboarding/role');

    // Auto-set default personas for new users to streamline flow
    if (!onboardingCompleted && selectedPersonas.length === 0) {
      setSelectedPersonas(getPersonasForInterview(interviewerMode, interviewerPersonality, currentIndustry));
    }
  }, [onboardingCompleted]);

  const [roleTitle, setRoleTitle] = useState(currentRole || 'Customer Support Representative');
  const [seniority, setSeniority] = useState(currentSeniority);
  const [companyName, setCompanyName] = useState(currentCompanyName);
  const [companyWebsite, setCompanyWebsite] = useState(currentCompanyWebsite);
  const [companyContext, setCompanyContext] = useState(currentCompanyContext);
  const [isIndustryModalVisible, setIsIndustryModalVisible] = useState(false);
  const [customIndustry, setCustomIndustry] = useState('');
  const [selectingOther, setSelectingOther] = useState(false);

  const industries = [
    'Business / Administration',
    'Customer Service',
    'Sales',
    'Marketing',
    'Finance',
    'Healthcare',
    'Education',
    'Hospitality',
    'Retail / E-commerce',
    'Operations',
    'Human Resources',
    'Legal',
    'Tech / Software',
    'Manufacturing',
    'Government',
    'Other'
  ];

  const canSubmit = roleTitle.trim().length >= 2;

  const submit = () => {
    if (!canSubmit) return;
    setRoleDetails(roleTitle.trim(), seniority);
    setCompanyDetails(companyName.trim(), companyWebsite.trim(), companyContext.trim());
    setSelectedPersonas(getPersonasForInterview(interviewerMode, interviewerPersonality, currentIndustry));
    router.push('/onboarding/mic-permission');
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.headerNav}>
        <Pressable
          onPress={() => {
            if (router.canGoBack()) {
              router.back();
            } else {
              router.replace('/');
            }
          }}
          style={styles.backButton}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        >
          <Feather name="chevron-left" size={24} color={colors.accent} />
        </Pressable>
        <Text style={styles.progressText}>
          {onboardingCompleted ? 'NEW INTERVIEW' : `STEP 2 / 2`}
        </Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.headerText}>
          <Text style={styles.title}>{t.onboarding.roleHeadline}</Text>
          <Text style={styles.subtitle}>{t.onboarding.roleSubheadline}</Text>
        </View>

        <View style={styles.form}>
          <View style={styles.inputGroup}>
            <Text style={styles.label}>ROLE YOU WANT</Text>
            <TextInput
              style={styles.input}
              placeholder="Senior Product Manager"
              placeholderTextColor="rgba(255, 255, 255, 0.3)"
              value={roleTitle}
              onChangeText={setRoleTitle}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>INDUSTRY</Text>
            <Pressable
              onPress={() => setIsIndustryModalVisible(true)}
              style={({ pressed }) => [
                styles.dropdownPlaceholder,
                pressed && { opacity: 0.7 }
              ]}
            >
              <Text style={styles.dropdownText}>{currentIndustry}</Text>
              <Feather name="chevron-down" size={20} color={colors.accent} />
            </Pressable>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>EXPERIENCE LEVEL</Text>
            <View style={styles.segmentedControl}>
              {seniorityLevels.map((level) => (
                <Pressable
                  key={level}
                  onPress={() => setSeniority(level)}
                  style={[
                    styles.segment,
                    seniority === level && styles.segmentActive
                  ]}
                >
                  <Text style={[
                    styles.segmentText,
                    seniority === level && styles.segmentTextActive
                  ]}>
                    {level}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <View style={styles.companyCard}>
            <View style={styles.companyHeader}>
              <View style={styles.companyIcon}>
                <Feather name="search" size={18} color={colors.accent} />
              </View>
              <View style={styles.companyHeaderCopy}>
                <Text style={styles.companyTitle}>Company or job details</Text>
                <Text style={styles.companySubtitle}>Optional. Helps the app ask better questions.</Text>
              </View>
            </View>

            <TextInput
              style={styles.companyInput}
              placeholder="Company name, e.g. Stripe or Goldman Sachs"
              placeholderTextColor="rgba(255, 255, 255, 0.3)"
              value={companyName}
              onChangeText={setCompanyName}
            />

            <TextInput
              style={styles.companyInput}
              placeholder="Website or job link"
              placeholderTextColor="rgba(255, 255, 255, 0.3)"
              value={companyWebsite}
              onChangeText={setCompanyWebsite}
              autoCapitalize="none"
              keyboardType="url"
            />

            <TextInput
              style={[styles.companyInput, styles.companyTextarea]}
              placeholder="Paste job description, company notes, products, market, or interview focus."
              placeholderTextColor="rgba(255, 255, 255, 0.3)"
              value={companyContext}
              onChangeText={setCompanyContext}
              multiline
              textAlignVertical="top"
            />

            <Text style={styles.helperText}>
              Paste details for the best results. Online research can add public company context when enabled.
            </Text>

            <View style={styles.webResearchRow}>
              <View style={styles.webResearchCopy}>
                <View style={styles.webResearchTitleRow}>
                  <Text style={styles.webResearchTitle}>Use online company research</Text>
                  {!hasPremiumAccess ? <Text style={styles.proBadge}>PRO</Text> : null}
                </View>
                <Text style={styles.webResearchSubtitle}>
                  {hasPremiumAccess
                    ? 'Adds public company context when available.'
                    : 'Paste details for free, or turn on company research later.'}
                </Text>
              </View>
              <Switch
                value={hasPremiumAccess && enableWebResearch}
                onValueChange={(value) => setEnableWebResearch(hasPremiumAccess ? value : false)}
                disabled={!hasPremiumAccess}
                trackColor={{ false: colors.border, true: colors.accent }}
                thumbColor={colors.text}
              />
            </View>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>WHO SHOULD ASK QUESTIONS?</Text>
            <View style={styles.segmentedControl}>
              {(['single', 'panel'] as InterviewerMode[]).map((mode) => (
                <Pressable
                  key={mode}
                  onPress={() => {
                    setInterviewerMode(mode);
                    setSelectedPersonas(getPersonasForInterview(mode, interviewerPersonality, currentIndustry));
                  }}
                  style={[
                    styles.segment,
                    interviewerMode === mode && styles.segmentActive
                  ]}
                >
                  <Text style={[
                    styles.segmentText,
                    interviewerMode === mode && styles.segmentTextActive
                  ]}>
                    {mode === 'single' ? 'Single' : 'Panel'}
                  </Text>
                </Pressable>
              ))}
            </View>
            <Text style={styles.helperText}>
              Start with one interviewer for speed, or use panel mode when you want multiple interviewer styles.
            </Text>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>INTERVIEWER STYLE</Text>
            <View style={styles.personalityGrid}>
              {([
                ['friendly', 'Supportive'],
                ['neutral', 'Balanced'],
                ['tough', 'Challenging'],
              ] as Array<[InterviewerPersonality, string]>).map(([value, label]) => (
                <Pressable
                  key={value}
                  onPress={() => {
                    if (!hasPremiumAccess && value !== 'neutral') {
                      router.push('/(modals)/paywall' as any);
                      return;
                    }
                    setInterviewerPersonality(value);
                    setSelectedPersonas(getPersonasForInterview(interviewerMode, value, currentIndustry));
                  }}
                  style={[
                    styles.personalityChip,
                    interviewerPersonality === value && styles.personalityChipActive
                  ]}
                >
                  <Text style={[
                    styles.personalityText,
                    interviewerPersonality === value && styles.personalityTextActive
                  ]}>
                    {!hasPremiumAccess && value !== 'neutral' ? `${label} Pro` : label}
                  </Text>
                </Pressable>
              ))}
            </View>
            <Text style={styles.helperText}>
              The interviewer look follows the industry: tech is more casual, law and finance are more formal.
            </Text>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>QUESTION DIFFICULTY</Text>
            <View style={styles.personalityGrid}>
              {(['Beginner', 'Realistic', 'Pressure'] as const).map((level) => (
                <Pressable
                  key={level}
                  onPress={() => setDifficulty(level)}
                  style={[
                    styles.personalityChip,
                    difficulty === level && styles.personalityChipActive
                  ]}
                >
                  <Text style={[
                    styles.personalityText,
                    difficulty === level && styles.personalityTextActive
                  ]}>
                    {level}
                  </Text>
                </Pressable>
              ))}
            </View>
            <Text style={styles.helperText}>
              Beginner is gentle, Realistic feels like a normal interview, Pressure adds tougher follow-ups.
            </Text>
          </View>

          <View style={styles.inputGroup}>
            <Text style={styles.label}>YOUR PREVIEW IMAGE</Text>
            <View style={styles.segmentedControl}>
              {['female', 'male'].map((g) => (
                <Pressable
                  key={g}
                  onPress={() => setUserGender(g as UserGender)}
                  style={[
                    styles.segment,
                    userGender === g && styles.segmentActive
                  ]}
                >
                  <Text style={[
                    styles.segmentText,
                    userGender === g && styles.segmentTextActive
                  ]}>
                    {g === 'female' ? 'Female' : 'Male'}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>

          <View style={styles.toggleRow}>
            <View style={styles.toggleCopy}>
              <Text style={styles.toggleTitle}>Ask role-specific skill questions</Text>
              <Text style={styles.toggleSubtitle}>Useful for software, finance, healthcare, law, sales, and other specialist roles.</Text>
            </View>
            <Switch
              value={isTechnical}
              onValueChange={setIsTechnical}
              trackColor={{ false: colors.border, true: colors.accent }}
              thumbColor={colors.text}
            />
          </View>

        </View>
      </ScrollView>

      <Modal
        visible={isIndustryModalVisible}
        transparent={true}
        animationType="slide"
        onRequestClose={() => setIsIndustryModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <Pressable
            style={styles.modalDismiss}
            onPress={() => setIsIndustryModalVisible(false)}
          />
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <View style={styles.modalHandle} />
              <Text style={styles.modalTitle}>{selectingOther ? 'Custom Industry' : 'Select Industry'}</Text>
            </View>

            {selectingOther ? (
              <View style={styles.otherInputContainer}>
                <TextInput
                  style={styles.otherInput}
                  placeholder="e.g. Quantum Computing"
                  placeholderTextColor="rgba(255, 255, 255, 0.3)"
                  value={customIndustry}
                  onChangeText={setCustomIndustry}
                  autoFocus
                />
                <View style={styles.otherActions}>
                  <Pressable
                    onPress={() => setSelectingOther(false)}
                    style={styles.cancelBtn}
                  >
                    <Text style={styles.cancelBtnText}>Back</Text>
                  </Pressable>
                  <Pressable
                    onPress={() => {
                      if (customIndustry.trim()) {
                        setStoreIndustry(customIndustry.trim());
                        setIsIndustryModalVisible(false);
                        setSelectingOther(false);
                      }
                    }}
                    style={[
                      styles.confirmBtn,
                      !customIndustry.trim() && { opacity: 0.5 }
                    ]}
                    disabled={!customIndustry.trim()}
                  >
                    <Text style={styles.confirmBtnText}>Confirm</Text>
                  </Pressable>
                </View>
              </View>
            ) : (
              <FlatList
                data={industries}
                keyExtractor={(item) => item}
                renderItem={({ item }) => (
                  <Pressable
                    onPress={() => {
                      if (item === 'Other') {
                        setSelectingOther(true);
                      } else {
                        setStoreIndustry(item);
                        setIsIndustryModalVisible(false);
                      }
                    }}
                    style={({ pressed }) => [
                      styles.industryItem,
                      currentIndustry === item && styles.industryItemActive,
                      pressed && { backgroundColor: 'rgba(108, 99, 255, 0.1)' }
                    ]}
                  >
                    <Text style={[
                      styles.industryItemText,
                      currentIndustry === item && styles.industryItemTextActive
                    ]}>
                      {item}
                    </Text>
                    {currentIndustry === item && (
                      <Feather name="check" size={20} color={colors.accent} />
                    )}
                  </Pressable>
                )}
                contentContainerStyle={styles.modalList}
              />
            )}
          </View>
        </View>
      </Modal>

      <View style={styles.footer}>
        <Pressable
          onPress={submit}
          disabled={!canSubmit}
          style={({ pressed }) => [
            styles.continueButton,
            !canSubmit && styles.continueButtonDisabled,
            pressed && { opacity: 0.8 }
          ]}
        >
          <Text style={styles.continueText}>Continue</Text>
          <Feather name="zap" size={20} color={colors.text} style={styles.continueIcon} />
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
    height: 80,
  },
  backButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.card,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  headerSpacer: {
    width: 44,
  },
  progressText: {
    color: colors.textMuted,
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  scrollContent: {
    paddingHorizontal: 24,
    paddingBottom: 120,
  },
  headerText: {
    marginTop: 20,
    marginBottom: 32,
  },
  title: {
    color: colors.text,
    fontSize: 32,
    fontWeight: '900',
    lineHeight: 40,
    letterSpacing: -1,
  },
  subtitle: {
    color: colors.textMuted,
    fontSize: 16,
    marginTop: 8,
    lineHeight: 24,
  },
  form: {
    gap: 24,
  },
  inputGroup: {
    gap: 12,
  },
  label: {
    color: colors.textMuted,
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 2,
    marginLeft: 4,
  },
  input: {
    backgroundColor: colors.card,
    borderRadius: 24,
    height: 64,
    paddingHorizontal: 24,
    color: colors.text,
    fontSize: 17,
    fontWeight: '700',
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  dropdownPlaceholder: {
    backgroundColor: colors.card,
    borderRadius: 24,
    height: 64,
    paddingHorizontal: 24,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  dropdownText: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '700',
  },
  segmentedControl: {
    backgroundColor: colors.input,
    borderRadius: 24,
    height: 68,
    flexDirection: 'row',
    padding: 6,
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  personalityGrid: {
    flexDirection: 'row',
    gap: 10,
  },
  personalityChip: {
    flex: 1,
    minHeight: 54,
    borderRadius: 18,
    backgroundColor: colors.input,
    borderWidth: 1,
    borderColor: colors.glassLight,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
  },
  personalityChipActive: {
    backgroundColor: colors.accent,
    borderColor: colors.accent,
  },
  personalityText: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '800',
    textAlign: 'center',
    lineHeight: 16,
    includeFontPadding: false,
  },
  personalityTextActive: {
    color: colors.text,
  },
  helperText: {
    color: colors.textMuted,
    fontSize: 12,
    lineHeight: 18,
    marginLeft: 4,
  },
  companyCard: {
    backgroundColor: colors.card,
    borderRadius: 26,
    borderWidth: 1,
    borderColor: colors.glassLight,
    padding: 18,
    gap: 12,
  },
  companyHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 2,
  },
  companyIcon: {
    width: 42,
    height: 42,
    borderRadius: 16,
    backgroundColor: colors.accentGlass,
    alignItems: 'center',
    justifyContent: 'center',
  },
  companyHeaderCopy: {
    flex: 1,
  },
  companyTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '900',
  },
  companySubtitle: {
    color: colors.textMuted,
    fontSize: 13,
    lineHeight: 18,
    marginTop: 2,
  },
  companyInput: {
    backgroundColor: colors.input,
    borderRadius: 18,
    minHeight: 54,
    paddingHorizontal: 16,
    paddingVertical: 14,
    color: colors.text,
    fontSize: 15,
    fontWeight: '700',
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  companyTextarea: {
    minHeight: 118,
    lineHeight: 21,
  },
  webResearchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    backgroundColor: colors.accentGlass,
    borderRadius: 18,
    padding: 14,
    marginTop: 2,
  },
  webResearchCopy: {
    flex: 1,
  },
  webResearchTitle: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '900',
  },
  webResearchTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flexWrap: 'wrap',
  },
  proBadge: {
    color: colors.text,
    backgroundColor: colors.accent,
    borderRadius: 8,
    overflow: 'hidden',
    paddingHorizontal: 7,
    paddingVertical: 2,
    fontSize: 9,
    fontWeight: '900',
  },
  webResearchSubtitle: {
    color: colors.textMuted,
    fontSize: 12,
    lineHeight: 17,
    marginTop: 2,
  },
  segment: {
    flex: 1,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  segmentActive: {
    backgroundColor: colors.accent,
    shadowColor: colors.accent,
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 4,
  },
  segmentText: {
    color: colors.textMuted,
    fontSize: 13,
    fontWeight: '800',
    lineHeight: 18,
    textAlign: 'center',
    includeFontPadding: false,
  },
  segmentTextActive: {
    color: colors.text,
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.accentGlass,
    padding: 24,
    borderRadius: 28,
    marginTop: 8,
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.1)',
  },
  toggleCopy: {
    flex: 1,
  },
  toggleTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '800',
  },
  toggleSubtitle: {
    color: colors.textMuted,
    fontSize: 14,
    marginTop: 2,
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    paddingHorizontal: 24,
    paddingBottom: 40,
    backgroundColor: 'rgba(0,0,0,0.8)',
    paddingTop: 20,
  },
  continueButton: {
    backgroundColor: colors.accent,
    height: 64,
    borderRadius: 32,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 28,
    shadowColor: colors.accent,
    shadowOpacity: 0.4,
    shadowRadius: 15,
    elevation: 10,
  },
  continueButtonDisabled: {
    opacity: 0.45,
  },
  continueText: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '800',
    lineHeight: 23,
    includeFontPadding: false,
  },
  continueIcon: {
    marginLeft: 10,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'flex-end',
  },
  modalDismiss: {
    flex: 1,
  },
  modalContent: {
    backgroundColor: colors.background,
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    maxHeight: '60%',
    paddingBottom: 40,
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  modalHeader: {
    alignItems: 'center',
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: colors.glassLight,
  },
  modalHandle: {
    width: 40,
    height: 4,
    backgroundColor: colors.glassLight,
    borderRadius: 2,
    marginBottom: 12,
  },
  modalTitle: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '900',
    letterSpacing: 1,
    textTransform: 'uppercase',
  },
  modalList: {
    padding: 16,
  },
  industryItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 18,
    paddingHorizontal: 24,
    borderRadius: 20,
    marginBottom: 4,
  },
  industryItemActive: {
    backgroundColor: 'rgba(108, 99, 255, 0.05)',
  },
  industryItemText: {
    color: colors.textMuted,
    fontSize: 17,
    fontWeight: '700',
  },
  industryItemTextActive: {
    color: colors.accent,
    fontWeight: '900',
  },
  otherInputContainer: {
    padding: 24,
    gap: 20,
  },
  otherInput: {
    backgroundColor: colors.card,
    borderRadius: 20,
    height: 64,
    paddingHorizontal: 24,
    color: colors.text,
    fontSize: 17,
    fontWeight: '700',
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  otherActions: {
    flexDirection: 'row',
    gap: 12,
  },
  cancelBtn: {
    flex: 1,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.glassLight,
  },
  cancelBtnText: {
    color: colors.textMuted,
    fontSize: 16,
    fontWeight: '800',
  },
  confirmBtn: {
    flex: 2,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.accent,
  },
  confirmBtnText: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '800',
  },
});
