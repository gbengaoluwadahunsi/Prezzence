import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useState } from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  View,
  Pressable,
  TextInput,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { apiClient } from '../../src/api/client';
import { supabase } from '../../src/lib/supabase';
import { useAuthStore } from '../../src/store/authStore';

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  textSecondary: '#8A8A9A',
  card: '#1C1C2E',
  success: '#00D68F',
  danger: '#FF4757',
  border: '#2A2A3E',
};

export default function DeleteAccountScreen() {
  const [confirmText, setConfirmText] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const signOut = useAuthStore((state) => state.signOut);

  const handleDeleteAccount = async () => {
    if (confirmText !== 'DELETE' || loading) return;

    setLoading(true);
    setErrorMessage('');
    try {
      await apiClient.delete('/api/users/me', { timeoutMs: 30000 });
      await supabase.auth.signOut();
      signOut();
      router.replace('/auth/sign-in' as any);
    } catch (error: any) {
      const message = error?.message || 'Unable to delete account right now.';
      setErrorMessage(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={styles.circleBtn}>
            <Feather name="chevron-left" size={24} color="#FFFFFF" />
          </Pressable>
          <Text style={styles.headerTitle}>Delete Account</Text>
          <Pressable onPress={() => router.push('/profile/privacy' as any)} style={styles.circleBtn}>
            <Feather name="shield" size={22} color="#FFFFFF" />
          </Pressable>
        </View>

        {/* Progress Tracker */}
        <View style={styles.progressBar}>
          {[...Array(7)].map((_, i) => (
            <View
              key={i}
              style={[styles.progressSegment, i < 3 && styles.progressSegmentActive]}
            />
          ))}
        </View>

        {/* Title */}
        <View style={styles.titleSection}>
          <Text style={styles.headline}>Delete Account</Text>
          <Text style={styles.subheadline}>Type DELETE to permanently remove data.</Text>
        </View>

        {/* Warning Card */}
        <View style={styles.warningCard}>
          <Text style={styles.cardTitle}>This deletes all sessions</Text>
          <Text style={styles.cardText}>
            Scores, recordings, progress history, and saved interview settings will be removed.
          </Text>
        </View>

        {/* Confirm Input */}
        <View style={styles.inputWrapper}>
          <Text style={styles.inputLabel}>CONFIRM</Text>
          <View style={styles.inputContainer}>
            <TextInput
              placeholder="Type DELETE"
              placeholderTextColor="rgba(255,255,255,0.3)"
              style={styles.textInput}
              autoCapitalize="characters"
              value={confirmText}
              onChangeText={setConfirmText}
            />
          </View>
        </View>
      </ScrollView>

      {/* Footer */}
      <View style={styles.footer}>
        <Pressable
          disabled={confirmText !== 'DELETE' || loading}
          onPress={handleDeleteAccount}
          style={({ pressed }) => [
            styles.dangerButton,
            (confirmText !== 'DELETE' || loading) && { opacity: 0.5 },
            pressed && confirmText === 'DELETE' && { opacity: 0.9 }
          ]}
        >
          {loading ? <ActivityIndicator color="#FFFFFF" /> : <Text style={styles.buttonText}>Delete Account</Text>}
        </Pressable>
        {errorMessage ? <Text style={styles.errorText}>{errorMessage}</Text> : null}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bgPrimary,
  },
  scrollContent: {
    paddingBottom: 120,
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
    backgroundColor: 'rgba(28, 28, 46, 0.6)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '800',
  },
  progressBar: {
    flexDirection: 'row',
    paddingHorizontal: 24,
    height: 4,
    marginTop: 8,
    gap: 8,
  },
  progressSegment: {
    flex: 1,
    backgroundColor: '#1C1C2E',
    borderRadius: 2,
  },
  progressSegmentActive: {
    backgroundColor: Colors.accent,
  },
  titleSection: {
    paddingHorizontal: 24,
    marginTop: 32,
    marginBottom: 40,
  },
  headline: {
    color: '#FFFFFF',
    fontSize: 48,
    fontWeight: '900',
    letterSpacing: -1,
  },
  subheadline: {
    color: Colors.textSecondary,
    fontSize: 16,
    marginTop: 8,
    lineHeight: 24,
  },
  warningCard: {
    backgroundColor: Colors.card,
    borderRadius: 24,
    padding: 32,
    marginHorizontal: 20,
    borderWidth: 1,
    borderColor: 'rgba(108, 99, 255, 0.4)',
    marginBottom: 32,
  },
  cardTitle: {
    color: '#FFFFFF',
    fontSize: 22,
    fontWeight: '800',
    marginBottom: 12,
  },
  cardText: {
    color: Colors.textSecondary,
    fontSize: 18,
    lineHeight: 28,
  },
  inputWrapper: {
    paddingHorizontal: 20,
  },
  inputLabel: {
    color: Colors.textSecondary,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 2,
    marginBottom: 12,
    paddingLeft: 4,
  },
  inputContainer: {
    backgroundColor: Colors.card,
    height: 64,
    borderRadius: 16,
    paddingHorizontal: 20,
    justifyContent: 'center',
  },
  textInput: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: 24,
    paddingBottom: 34,
  },
  dangerButton: {
    backgroundColor: Colors.danger,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: Colors.danger,
    shadowOpacity: 0.3,
    shadowRadius: 15,
    elevation: 8,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '700',
  },
  errorText: {
    color: '#FFB4BE',
    fontSize: 13,
    lineHeight: 18,
    textAlign: 'center',
    marginTop: 10,
    fontWeight: '600',
  },
});
