import { Feather } from '@expo/vector-icons';
import { router, useLocalSearchParams } from 'expo-router';
import React, { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useSessionStore } from '../../src/store/sessionStore';
import { useTranslation } from '../../src/hooks/useTranslation';

const VIOLET = '#5C61FF';
const BG_DARK = '#000000';
const MUTED_TEXT = '#A5A6BA';
const CARD_BG = '#171721';

const languages = [
  { id: 'en', name: 'English', native: 'English', flag: 'US' },
  { id: 'es', name: 'Spanish', native: 'Espanol', flag: 'ES' },
  { id: 'fr', name: 'French', native: 'Francais', flag: 'FR' },
  { id: 'de', name: 'German', native: 'Deutsch', flag: 'DE' },
  { id: 'it', name: 'Italian', native: 'Italiano', flag: 'IT' },
  { id: 'pt', name: 'Portuguese', native: 'Portugues', flag: 'BR' },
  { id: 'zh', name: 'Chinese', native: 'Zhongwen', flag: 'CN' },
  { id: 'ja', name: 'Japanese', native: 'Nihongo', flag: 'JP' },
  { id: 'ko', name: 'Korean', native: 'Hangugeo', flag: 'KR' },
  { id: 'ar', name: 'Arabic', native: 'Al-Arabiyyah', flag: 'SA' },
  { id: 'hi', name: 'Hindi', native: 'Hindi', flag: 'IN' },
];

const saveLanguageLabels: Record<string, string> = {
  en: 'Save language',
  es: 'Guardar idioma',
  fr: 'Enregistrer la langue',
  de: 'Sprache speichern',
  it: 'Salva lingua',
  pt: 'Salvar idioma',
  zh: '保存语言',
  ja: '言語を保存',
  ko: '언어 저장',
  ar: 'حفظ اللغة',
  hi: 'भाषा सहेजें',
};

export default function LanguageScreen() {
  const { language, setLanguage } = useSessionStore();
  const { t } = useTranslation();
  const params = useLocalSearchParams<{ returnTo?: string }>();
  const [search, setSearch] = useState('');
  const shouldReturnBack = params.returnTo === 'back';

  const filteredLanguages = languages.filter((item) =>
    item.name.toLowerCase().includes(search.toLowerCase()) ||
    item.native.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.headerNav}>
        <Pressable onPress={() => router.back()} style={styles.backButton}>
          <Feather name="chevron-left" size={24} color="#FFFFFF" />
        </Pressable>
        <View style={styles.headerCenter}>
          <Text style={styles.title}>{t.onboarding.languageTitle}</Text>
        </View>
        <View style={styles.headerRight} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        <View style={styles.heroLine}>
          <Text style={styles.headline}>{t.onboarding.languageHeadline}</Text>
          <Text style={styles.subheadline}>{t.onboarding.languageSubheadline}</Text>
        </View>

        <View style={styles.searchContainer}>
          <Feather name="search" size={18} color={MUTED_TEXT} style={styles.searchIcon} />
          <TextInput
            placeholder={t.common.search}
            placeholderTextColor={MUTED_TEXT}
            style={styles.searchInput}
            value={search}
            onChangeText={setSearch}
          />
        </View>

        <View style={styles.grid}>
          {filteredLanguages.map((item) => (
            <Pressable
              key={item.id}
              onPress={() => setLanguage(item.id)}
              style={[styles.card, language === item.id && styles.cardActive]}
            >
              <Text style={styles.flag}>{item.flag}</Text>
              <View>
                <Text style={styles.langName}>{item.name}</Text>
                <Text style={styles.langNative}>{item.native}</Text>
              </View>
              {language === item.id && (
                <View style={styles.checkCircle}>
                  <Feather name="check" size={12} color="#FFFFFF" />
                </View>
              )}
            </Pressable>
          ))}
        </View>
      </ScrollView>

      <View style={styles.footer}>
        <Pressable
          onPress={() => shouldReturnBack ? router.back() : router.push('/auth/sign-up' as any)}
          style={({ pressed }) => [styles.continueButton, pressed && { opacity: 0.8 }]}
        >
          <Text style={styles.continueText} numberOfLines={1} adjustsFontSizeToFit>
            {shouldReturnBack ? saveLanguageLabels[language] || saveLanguageLabels.en : t.common.continue}
          </Text>
          <Feather name="chevron-right" size={18} color="#FFFFFF" style={{ marginLeft: 10 }} />
        </Pressable>
      </View>
    </SafeAreaView>
  );
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
    height: 60,
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#1C1C2E',
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerCenter: {
    alignItems: 'center',
  },
  title: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '800',
    marginTop: 4,
  },
  headerRight: {
    width: 44,
  },
  scrollContent: {
    paddingHorizontal: 24,
    paddingBottom: 120,
  },
  heroLine: {
    marginTop: 12,
    marginBottom: 20,
  },
  headline: {
    color: '#FFFFFF',
    fontSize: 32,
    fontWeight: '900',
    lineHeight: 40,
  },
  subheadline: {
    color: MUTED_TEXT,
    fontSize: 16,
    marginTop: 8,
    lineHeight: 24,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: CARD_BG,
    borderRadius: 16,
    paddingHorizontal: 16,
    height: 56,
    marginBottom: 24,
  },
  searchIcon: {
    marginRight: 12,
  },
  searchInput: {
    flex: 1,
    color: '#FFFFFF',
    fontSize: 16,
  },
  grid: {
    gap: 12,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: CARD_BG,
    borderRadius: 20,
    padding: 16,
    borderWidth: 2,
    borderColor: 'transparent',
  },
  cardActive: {
    borderColor: VIOLET,
    backgroundColor: 'rgba(92, 97, 255, 0.05)',
  },
  flag: {
    minWidth: 32,
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '900',
    marginRight: 16,
    textAlign: 'center',
  },
  langName: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  langNative: {
    color: MUTED_TEXT,
    fontSize: 12,
    marginTop: 2,
  },
  checkCircle: {
    position: 'absolute',
    right: 16,
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: VIOLET,
    alignItems: 'center',
    justifyContent: 'center',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    paddingHorizontal: 24,
    paddingBottom: 34,
  },
  continueButton: {
    backgroundColor: VIOLET,
    height: 56,
    borderRadius: 28,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  continueText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '700',
  },
});
