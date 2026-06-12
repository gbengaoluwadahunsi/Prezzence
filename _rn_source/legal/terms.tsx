import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

const Colors = {
  bgPrimary: '#0A0A0F',
  textSecondary: '#A4A4B5',
  card: '#1C1C2E',
};

const sections = [
  {
    title: 'Service',
    body: 'Prezzence provides interview practice, AI-generated questions, voice-led practice sessions, transcription, scoring, and coaching insights. It does not guarantee job offers, admissions, or hiring outcomes.',
  },
  {
    title: 'User responsibilities',
    body: 'Use the app lawfully, submit only content you have the right to share, and do not upload confidential employer, client, patient, or regulated information unless you are authorized to do so.',
  },
  {
    title: 'AI feedback',
    body: 'Scores and feedback are coaching signals, not professional certification. Review important career decisions with human mentors, recruiters, or qualified advisors.',
  },
  {
    title: 'Subscriptions',
    body: 'Paid features should be purchased through Apple App Store or Google Play Store billing in production mobile builds. Subscription terms, trials, renewals, and cancellation controls must match the store listing.',
  },
  {
    title: 'Account deletion',
    body: 'You can delete your account from Profile. Deletion removes Prezzence app data associated with the authenticated account, subject to legal and fraud-prevention retention requirements.',
  },
];

export default function TermsScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={styles.circleBtn}>
            <Feather name="chevron-left" size={24} color="#FFFFFF" />
          </Pressable>
          <Text style={styles.headerTitle}>Terms</Text>
          <View style={styles.circleBtnPlaceholder} />
        </View>

        <Text style={styles.headline}>Terms of Service</Text>
        <Text style={styles.updated}>Last updated: May 21, 2026</Text>

        {sections.map((section) => (
          <View key={section.title} style={styles.card}>
            <Text style={styles.sectionTitle}>{section.title}</Text>
            <Text style={styles.body}>{section.body}</Text>
          </View>
        ))}
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
  headline: {
    color: '#FFFFFF',
    fontSize: 42,
    fontWeight: '900',
    paddingHorizontal: 24,
    marginTop: 18,
  },
  updated: {
    color: Colors.textSecondary,
    fontSize: 14,
    fontWeight: '700',
    paddingHorizontal: 24,
    marginTop: 8,
    marginBottom: 20,
  },
  card: {
    marginHorizontal: 20,
    marginBottom: 12,
    padding: 20,
    borderRadius: 22,
    backgroundColor: Colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.07)',
  },
  sectionTitle: { color: '#FFFFFF', fontSize: 18, fontWeight: '900', marginBottom: 8 },
  body: { color: Colors.textSecondary, fontSize: 15, lineHeight: 23, fontWeight: '600' },
});
