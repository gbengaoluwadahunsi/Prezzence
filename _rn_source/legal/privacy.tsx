import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  textSecondary: '#A4A4B5',
  card: '#1C1C2E',
};

const sections = [
  {
    title: 'What Prezzence collects',
    body: 'Account email, basic profile details, interview setup choices, optional resume or CV profile, audio you choose to record, transcripts, AI scores, feedback, crash diagnostics, and usage events needed to improve reliability.',
  },
  {
    title: 'How recordings and transcripts are used',
    body: 'Audio may be uploaded to speech and scoring providers so the app can transcribe answers, score interview quality, and generate coaching feedback. Recordings are used only for your interview practice experience and product safety.',
  },
  {
    title: 'Resume and CV uploads',
    body: 'Resume upload is optional. If you upload a PDF, DOCX, TXT, or Markdown resume, Prezzence sends it to the backend to extract a compact practice profile such as summary, skills, experience, and education. The original file is not kept as a stored document after parsing, and you can remove the stored resume profile from Profile.',
  },
  {
    title: 'Data retention and deletion',
    body: 'You can request account deletion from Profile. Deletion removes sessions, answers, stored resume profile, analytics events tied to your account, devices, and entitlements from Prezzence app storage.',
  },
  {
    title: 'Third-party processors',
    body: 'Prezzence may use Supabase for authentication, Neon for app data, Groq or Gemini for transcription and scoring, Edge TTS for voices, and object storage for generated audio files.',
  },
  {
    title: 'Contact',
    body: 'For privacy requests, contact the Prezzence support address listed in the store listing or company website before public launch.',
  },
];

export default function PrivacyPolicyScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={styles.circleBtn}>
            <Feather name="chevron-left" size={24} color="#FFFFFF" />
          </Pressable>
          <Text style={styles.headerTitle}>Privacy Policy</Text>
          <View style={styles.circleBtnPlaceholder} />
        </View>

        <Text style={styles.headline}>Privacy Policy</Text>
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
