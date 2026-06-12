import { Feather } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import {
  roleQuestionGroups,
  companyQuestionTemplates,
  companyProfiles,
  CompanyProfile,
  LibraryQuestion,
} from '../../src/data/questionLibrary';
import { useSessionStore } from '../../src/store/sessionStore';
import { createSession } from '../../src/api/sessions';
import { getPersonasForInterview } from '../../src/data/personas';
import { trackEvent } from '../../src/services/analytics';

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  textSecondary: '#9B9BAA',
  card: '#1C1C2E',
  border: '#2A2A3E',
  success: '#00D68F',
};

function QuestionCard({
  item,
  onPractice,
  loading,
}: {
  item: LibraryQuestion;
  onPractice: () => void;
  loading?: boolean;
}) {
  return (
    <View style={styles.questionCard}>
      <Text style={styles.questionText}>{item.question}</Text>
      <View style={styles.answerBox}>
        <Text style={styles.answerLabel}>What to include</Text>
        <Text style={styles.answerText}>{item.answer}</Text>
      </View>
      <Pressable
        onPress={onPractice}
        disabled={loading}
        style={[styles.practiceButton, loading && { opacity: 0.72 }]}
      >
        <Text style={styles.practiceButtonText}>
          {loading ? 'Starting practice...' : 'Practice this question'}
        </Text>
        {loading ? (
          <ActivityIndicator size="small" color="#FFFFFF" />
        ) : (
          <Feather name="arrow-right" size={16} color="#FFFFFF" />
        )}
      </Pressable>
    </View>
  );
}

function CompanyCard({
  company,
  onPress,
  loading,
}: {
  company: CompanyProfile;
  onPress: () => void;
  loading?: boolean;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={loading}
      style={({ pressed }) => [styles.companyDashboardCard, (pressed || loading) && { opacity: 0.86 }]}
    >
      <View style={styles.companyDashboardHeader}>
        <View style={styles.companyLogo}>
          <Feather name={company.icon as any} size={20} color="#FFFFFF" />
        </View>
        <View style={styles.companyDashboardCopy}>
          <Text style={styles.companyDashboardName}>{company.name}</Text>
          <Text style={styles.companyDashboardMeta}>{company.industry} - {company.region}</Text>
        </View>
        {loading ? (
          <ActivityIndicator size="small" color={Colors.accent} />
        ) : (
          <Feather name="arrow-right" size={18} color={Colors.accent} />
        )}
      </View>
      <Text style={styles.tapHint}>
        {loading ? 'Starting...' : `Practice ${company.name}-style questions`}
      </Text>
    </Pressable>
  );
}

export default function QuestionsScreen() {
  const params = useLocalSearchParams<{ custom?: string }>();
  const {
    roleTitle,
    seniority,
    setRoleDetails,
    setIndustry,
    setCompanyDetails,
    setInterviewType,
    setCompanyContext,
    setSessionResult,
    selectedPersonas,
    interviewerMode,
    interviewerPersonality,
    difficulty,
    length,
    language,
    isTechnical,
    resetSession,
  } = useSessionStore();
  const [search, setSearch] = React.useState('');
  const [company, setCompany] = React.useState('');
  const [customQuestion, setCustomQuestion] = React.useState('');
  const [selectedRole, setSelectedRole] = React.useState(() => {
    const savedMatch = roleQuestionGroups.find((group) => group.role === roleTitle);
    return savedMatch?.role || roleQuestionGroups[0].role;
  });
  const [startingCompanyName, setStartingCompanyName] = React.useState<string | null>(null);
  const [startingQuestion, setStartingQuestion] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!roleTitle) return;
    const savedMatch = roleQuestionGroups.find((group) => group.role === roleTitle);
    if (savedMatch) setSelectedRole(savedMatch.role);
  }, [roleTitle]);

  const filteredRoles = roleQuestionGroups.filter((group) => {
    const query = search.trim().toLowerCase();
    if (!query) return true;
    return `${group.role} ${group.industry}`.toLowerCase().includes(query);
  });
  const filteredCompanies = companyProfiles.filter((profile) => {
    const query = search.trim().toLowerCase();
    if (!query) return true;
    return `${profile.name} ${profile.industry} ${profile.focus.join(' ')}`.toLowerCase().includes(query);
  });

  const activeGroup = roleQuestionGroups.find((group) => group.role === selectedRole) || roleQuestionGroups[0];
  const typedCompany = company.trim();
  const companyQuestions = typedCompany ? companyQuestionTemplates(typedCompany, activeGroup.role) : [];
  const customModeRequested = params.custom === '1';

  const startPractice = async (question: string, group = activeGroup) => {
    if (startingQuestion || startingCompanyName) return;

    const questionContext = [
      'Single-question practice.',
      'After the short introduction, ask this exact practice question:',
      question,
      typedCompany ? `Company context: ${typedCompany}.` : '',
    ].filter(Boolean).join('\n');

    resetSession();
    setInterviewType('custom');
    setRoleDetails(group.role, seniority || 'Mid-level');
    setIndustry(group.industry);
    setCompanyDetails(typedCompany, '', `Practice this question: ${question}`);
    setCompanyContext(questionContext);

    const personas = selectedPersonas.length
      ? selectedPersonas
      : getPersonasForInterview(interviewerMode, interviewerPersonality, group.industry);
    const panelConfig = personas.map((persona, index) => ({
      persona_id: persona.id,
      seat: index === 0 ? 'center' : index === 1 ? 'left' : 'right',
    }));

    try {
      setStartingQuestion(question);
      const result = await createSession({
        role_title: group.role,
        industry: group.industry,
        seniority: seniority || 'Mid-level',
        interview_type: 'custom',
        difficulty: difficulty || 'Realistic',
        include_technical: isTechnical,
        length: length || 'quick',
        panel_config: panelConfig,
        company_name: typedCompany,
        company_website: '',
        company_context: questionContext,
        enable_web_research: false,
        language,
      });
      setSessionResult(result.session_id, result.questions);
      trackEvent('question_practice_started', {
        role: group.role,
        question,
        questionCount: result.questions?.length || 0,
      });
      router.push('/interview/entering' as any);
    } catch (error) {
      console.warn('[Questions] Failed to start question practice:', error);
      router.push('/errors/network-error' as any);
    } finally {
      setStartingQuestion(null);
    }
  };

  const startCompanyInterview = async (profile: CompanyProfile) => {
    if (startingCompanyName) return;

    const likelyContext = [
      `${profile.name}-style interview practice.`,
      `Likely focus: ${profile.focus.join(', ')}.`,
      `Common questions: ${profile.likelyQuestions.join(' ')}`,
      'These are practice prompts, not leaked or guaranteed interview questions.',
    ].join('\n');

    resetSession();
    setInterviewType('job');
    setRoleDetails(activeGroup.role, seniority || 'Mid-level');
    setIndustry(profile.industry);
    setCompanyDetails(profile.name, '', likelyContext);
    setCompanyContext(likelyContext);

    const personas = selectedPersonas.length
      ? selectedPersonas
      : getPersonasForInterview(interviewerMode, interviewerPersonality, profile.industry);
    const panelConfig = personas.map((persona, index) => ({
      persona_id: persona.id,
      seat: index === 0 ? 'center' : index === 1 ? 'left' : 'right',
    }));

    try {
      setStartingCompanyName(profile.name);
      const result = await createSession({
        role_title: activeGroup.role,
        industry: profile.industry,
        seniority: seniority || 'Mid-level',
        interview_type: 'job',
        difficulty: difficulty || 'Realistic',
        include_technical: isTechnical,
        length: length || 'standard',
        panel_config: panelConfig,
        company_name: profile.name,
        company_website: '',
        company_context: likelyContext,
        enable_web_research: false,
        language,
      });
      setSessionResult(result.session_id, result.questions);
      trackEvent('company_practice_started', {
        company: profile.name,
        role: activeGroup.role,
        questionCount: result.questions?.length || 0,
      });
      router.push('/interview/entering' as any);
    } catch (error) {
      console.warn('[Questions] Failed to start company interview:', error);
      router.push('/errors/network-error' as any);
    } finally {
      setStartingCompanyName(null);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <View>
            <Text style={styles.headerTitle}>Questions</Text>
            <Text style={styles.headerSubtitle}>Pick a role, choose one question, and start practicing.</Text>
          </View>
          <View style={styles.headerIcon}>
            <Feather name="help-circle" size={22} color={Colors.accent} />
          </View>
        </View>

        <View style={styles.searchBox}>
          <Feather name="search" size={18} color={Colors.textSecondary} />
          <TextInput
            value={search}
            onChangeText={setSearch}
            placeholder="Search role, company, or industry"
            placeholderTextColor={Colors.textSecondary}
            style={styles.searchInput}
          />
        </View>

        <Text style={styles.sectionTitle}>Choose a role</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.roleScroller}>
          {filteredRoles.map((group) => {
            const selected = group.role === activeGroup.role;
            return (
              <Pressable
                key={group.role}
                onPress={() => setSelectedRole(group.role)}
                style={[styles.rolePill, selected && styles.rolePillSelected]}
              >
                <Feather name={group.icon as any} size={16} color={selected ? '#FFFFFF' : Colors.textSecondary} />
                <Text style={[styles.rolePillText, selected && styles.rolePillTextSelected]}>{group.role}</Text>
              </Pressable>
            );
          })}
        </ScrollView>
        {filteredRoles.length === 0 ? (
          <View style={styles.emptyCard}>
            <Text style={styles.emptyTitle}>No role matches this search</Text>
            <Text style={styles.emptyText}>Try a broader role, company, or industry keyword.</Text>
          </View>
        ) : null}

        <Text style={styles.sectionTitle}>Practice questions</Text>
        <View style={styles.list}>
          {activeGroup.questions.map((item) => (
            <QuestionCard
              key={item.question}
              item={item}
              onPractice={() => startPractice(item.question)}
              loading={startingQuestion === item.question}
            />
          ))}
        </View>

        <View style={[styles.companyCard, customModeRequested && styles.highlightCard]}>
          <Text style={styles.companyTitle}>My own question</Text>
          <Text style={styles.companySubtitle}>Paste one question you want to practice. Prezzence will ask it and score your answer.</Text>
          <TextInput
            value={customQuestion}
            onChangeText={setCustomQuestion}
            placeholder="Example: Tell me about a time you handled a difficult customer."
            placeholderTextColor={Colors.textSecondary}
            style={[styles.companyInput, styles.customQuestionInput]}
            multiline
            textAlignVertical="top"
          />
          <Pressable
            onPress={() => startPractice(customQuestion.trim())}
            disabled={!customQuestion.trim() || Boolean(startingQuestion || startingCompanyName)}
            style={[
              styles.practiceButton,
              styles.customPracticeButton,
              (!customQuestion.trim() || Boolean(startingQuestion || startingCompanyName)) && { opacity: 0.5 },
            ]}
          >
            <Text style={styles.practiceButtonText}>
              {startingQuestion === customQuestion.trim() ? 'Starting practice...' : 'Practice my question'}
            </Text>
            {startingQuestion === customQuestion.trim() ? (
              <ActivityIndicator size="small" color="#FFFFFF" />
            ) : (
              <Feather name="arrow-right" size={16} color="#FFFFFF" />
            )}
          </Pressable>
        </View>

        <View style={styles.companyCard}>
          <Text style={styles.companyTitle}>Target a company</Text>
          <Text style={styles.companySubtitle}>Optional. Add a company name to tailor the practice question.</Text>
          <TextInput
            value={company}
            onChangeText={setCompany}
            placeholder="Company name, e.g. Google, KPMG, NHS"
            placeholderTextColor={Colors.textSecondary}
            style={styles.companyInput}
          />
        </View>

        <View style={styles.dashboardHeader}>
          <Text style={styles.sectionTitle}>Company practice</Text>
        </View>
        <View style={styles.companyGrid}>
          {filteredCompanies.slice(0, 6).map((profile) => (
            <CompanyCard
              key={profile.name}
              company={profile}
              onPress={() => startCompanyInterview(profile)}
              loading={startingCompanyName === profile.name}
            />
          ))}
        </View>

        {typedCompany ? (
          <>
            <Text style={styles.sectionTitle}>Likely questions for {typedCompany}</Text>
            <View style={styles.list}>
              {companyQuestions.map((item) => (
                <QuestionCard
                  key={item.question}
                  item={item}
                  onPractice={() => startPractice(item.question)}
                  loading={startingQuestion === item.question}
                />
              ))}
            </View>
          </>
        ) : null}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bgPrimary,
  },
  content: {
    paddingHorizontal: 20,
    paddingBottom: 120,
  },
  header: {
    minHeight: 104,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 16,
    paddingTop: 12,
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 34,
    fontWeight: '900',
    letterSpacing: -0.5,
  },
  headerSubtitle: {
    color: Colors.textSecondary,
    fontSize: 15,
    lineHeight: 21,
    fontWeight: '700',
    marginTop: 6,
    maxWidth: 285,
  },
  headerIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: 'rgba(108, 99, 255, 0.12)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchBox: {
    height: 54,
    borderRadius: 18,
    backgroundColor: 'rgba(255, 255, 255, 0.06)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.07)',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    gap: 10,
    marginBottom: 24,
  },
  searchInput: {
    flex: 1,
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
  sectionTitle: {
    color: '#FFFFFF',
    fontSize: 19,
    fontWeight: '900',
    marginBottom: 12,
  },
  sectionSubtitle: {
    color: Colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
    marginTop: -5,
    marginBottom: 14,
  },
  roleScroller: {
    gap: 10,
    paddingBottom: 22,
  },
  rolePill: {
    height: 44,
    borderRadius: 999,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 14,
    backgroundColor: Colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.07)',
  },
  rolePillSelected: {
    backgroundColor: Colors.accent,
    borderColor: Colors.accent,
  },
  rolePillText: {
    color: Colors.textSecondary,
    fontSize: 13,
    fontWeight: '900',
  },
  rolePillTextSelected: {
    color: '#FFFFFF',
  },
  emptyCard: {
    borderRadius: 20,
    padding: 16,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.07)',
    marginBottom: 22,
  },
  emptyTitle: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '900',
    marginBottom: 5,
  },
  emptyText: {
    color: Colors.textSecondary,
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
  },
  companyCard: {
    backgroundColor: Colors.card,
    borderRadius: 24,
    padding: 18,
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.18)',
    marginBottom: 28,
  },
  highlightCard: {
    borderColor: 'rgba(0, 214, 143, 0.45)',
    backgroundColor: 'rgba(0, 214, 143, 0.07)',
  },
  companyTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '900',
    marginBottom: 6,
  },
  companySubtitle: {
    color: Colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '700',
    marginBottom: 14,
  },
  companyInput: {
    minHeight: 52,
    borderRadius: 16,
    backgroundColor: 'rgba(0, 0, 0, 0.18)',
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
    paddingHorizontal: 14,
  },
  customQuestionInput: {
    minHeight: 104,
    paddingTop: 14,
    paddingBottom: 14,
    lineHeight: 21,
  },
  customPracticeButton: {
    marginTop: 14,
  },
  list: {
    gap: 16,
    marginBottom: 28,
  },
  dashboardHeader: {
    marginBottom: 2,
  },
  companyGrid: {
    gap: 14,
    marginBottom: 30,
  },
  companyDashboardCard: {
    backgroundColor: Colors.card,
    borderRadius: 24,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.07)',
  },
  companyDashboardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 12,
  },
  companyLogo: {
    width: 44,
    height: 44,
    borderRadius: 16,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  companyDashboardCopy: {
    flex: 1,
  },
  companyDashboardName: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '900',
  },
  companyDashboardMeta: {
    color: Colors.textSecondary,
    fontSize: 12,
    fontWeight: '800',
    marginTop: 3,
  },
  focusRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 12,
  },
  focusChip: {
    borderRadius: 999,
    backgroundColor: 'rgba(0, 214, 143, 0.12)',
    paddingHorizontal: 9,
    paddingVertical: 5,
  },
  focusChipText: {
    color: '#A9F5DD',
    fontSize: 10,
    fontWeight: '900',
  },
  companyQuestionsPreview: {
    gap: 7,
    marginBottom: 12,
  },
  previewQuestion: {
    color: '#FFFFFF',
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
    opacity: 0.9,
  },
  tapHint: {
    color: Colors.accent,
    fontSize: 12,
    fontWeight: '900',
  },
  questionCard: {
    backgroundColor: Colors.card,
    borderRadius: 24,
    padding: 18,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.07)',
  },
  questionText: {
    color: '#FFFFFF',
    fontSize: 18,
    lineHeight: 25,
    fontWeight: '900',
    marginBottom: 12,
  },
  tagRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 14,
  },
  tag: {
    borderRadius: 999,
    backgroundColor: 'rgba(108, 99, 255, 0.16)',
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  tagText: {
    color: '#C9C5FF',
    fontSize: 11,
    fontWeight: '900',
  },
  answerBox: {
    borderRadius: 18,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    padding: 14,
    marginBottom: 12,
  },
  answerLabel: {
    color: Colors.success,
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 0.8,
    textTransform: 'uppercase',
    marginBottom: 7,
  },
  answerText: {
    color: '#FFFFFF',
    fontSize: 14,
    lineHeight: 21,
    fontWeight: '700',
  },
  whyText: {
    color: Colors.textSecondary,
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '700',
    marginBottom: 14,
  },
  practiceButton: {
    height: 46,
    borderRadius: 999,
    backgroundColor: Colors.accent,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  practiceButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '900',
  },
});
