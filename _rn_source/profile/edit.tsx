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
  ActivityIndicator
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, spacing, typography, radii } from '../../src/theme';
import { useAuthStore } from '../../src/store/authStore';
import { supabase } from '../../src/lib/supabase';

function EditInput({ label, value, onChangeText, editable = true }: { label: string, value: string, onChangeText?: (t: string) => void, editable?: boolean }) {
  return (
    <View style={styles.inputGroup}>
      <Text style={styles.inputLabel}>{label}</Text>
      <View style={styles.inputBox}>
        <TextInput
          defaultValue={value}
          onChangeText={onChangeText}
          editable={editable}
          style={styles.textInput}
          placeholderTextColor="rgba(255,255,255,0.3)"
        />
      </View>
    </View>
  );
}

export default function EditProfileScreen() {
  const { user, setUser } = useAuthStore();
  const [name, setName] = useState(user?.user_metadata?.full_name || '');
  const [focus, setFocus] = useState(user?.user_metadata?.focus || '');
  const [saving, setSaving] = useState(false);

  const saveProfile = async () => {
    if (saving) return;
    setSaving(true);
    try {
      const { data, error } = await supabase.auth.updateUser({
        data: {
          full_name: name.trim(),
          focus: focus.trim(),
        },
      });
      if (error) throw error;
      if (data.user) setUser(data.user);
      router.back();
    } catch (error) {
      console.error('[EditProfile] Save failed:', error);
    } finally {
      setSaving(false);
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
          <Text style={styles.headerTitle}>Edit profile</Text>
          <View style={{ width: 44 }} />
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

        {/* Avatar Section */}
        <View style={styles.avatarHero}>
          <View style={styles.avatarHalo}>
            <View style={styles.avatarCircle}>
              <Feather name="user" size={48} color={colors.accent} />
            </View>
          </View>
          <Text style={styles.avatarHint}>{user?.email || 'Signed in account'}</Text>
        </View>

        {/* Form */}
        <View style={styles.form}>
          <EditInput label="NAME" value={name} onChangeText={setName} />
          <EditInput label="EMAIL" value={user?.email || ''} editable={false} />
          <EditInput label="INTERVIEW FOCUS" value={focus} onChangeText={setFocus} />
        </View>
      </ScrollView>

      {/* Footer */}
      <View style={styles.footer}>
        <Pressable
          onPress={saveProfile}
          disabled={saving}
          style={({ pressed }) => [
            styles.primaryButton,
            pressed && { opacity: 0.9 }
          ]}
        >
          {saving ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <>
              <Text style={styles.buttonText}>Save profile</Text>
              <Feather name="check" size={18} color="#FFFFFF" style={{ marginLeft: 10 }} />
            </>
          )}
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  scrollContent: {
    paddingBottom: 140,
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
    backgroundColor: colors.card,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
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
    backgroundColor: colors.accent,
  },
  avatarHero: {
    alignItems: 'center',
    marginTop: 32,
    marginBottom: 40,
  },
  avatarHalo: {
    width: 120,
    height: 120,
    borderRadius: 60,
    borderWidth: 1,
    borderColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 4,
  },
  avatarCircle: {
    width: '100%',
    height: '100%',
    borderRadius: 55,
    backgroundColor: colors.card,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cameraBtn: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: colors.background,
  },
  avatarHint: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 16,
    fontWeight: '600',
  },
  form: {
    paddingHorizontal: 20,
    gap: 24,
  },
  inputGroup: {
    gap: 12,
  },
  inputLabel: {
    color: colors.accent,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 2,
    paddingLeft: 4,
  },
  inputBox: {
    backgroundColor: colors.card,
    height: 64,
    borderRadius: 24,
    paddingHorizontal: 20,
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  textInput: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: 24,
    paddingBottom: 40,
    backgroundColor: 'rgba(0,0,0,0.8)',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.05)',
  },
  primaryButton: {
    backgroundColor: colors.accent,
    height: 64,
    borderRadius: 32,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: colors.accent,
    shadowOpacity: 0.3,
    shadowRadius: 15,
    elevation: 8,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '800',
  },
});
