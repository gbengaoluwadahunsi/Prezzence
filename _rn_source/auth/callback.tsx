import { router, useLocalSearchParams } from 'expo-router';
import React from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { clearSupabaseAuthStorage, supabase } from '../../src/lib/supabase';
import { routeAfterAuthSession } from '../../src/services/authNavigation';
import { useAuthStore } from '../../src/store/authStore';
import { colors } from '../../src/theme';

export default function AuthCallbackScreen() {
  const params = useLocalSearchParams<{
    code?: string;
    access_token?: string;
    refresh_token?: string;
    error?: string;
    error_code?: string;
    error_description?: string;
    type?: string;
  }>();
  const { setUser, setSession, setLoading } = useAuthStore();

  React.useEffect(() => {
    let mounted = true;

    const completeAuth = async () => {
      try {
        const error = params.error_description || params.error || params.error_code;
        if (error) {
          router.replace({
            pathname: '/auth/sign-in',
            params: { error: String(error) },
          } as any);
          return;
        }

        const authCode = typeof params.code === 'string' ? params.code : undefined;
        const accessToken = typeof params.access_token === 'string' ? params.access_token : undefined;
        const refreshToken = typeof params.refresh_token === 'string' ? params.refresh_token : undefined;

        const { data, error: authError } = authCode
          ? await supabase.auth.exchangeCodeForSession(authCode)
          : accessToken && refreshToken
            ? await supabase.auth.setSession({ access_token: accessToken, refresh_token: refreshToken })
            : await supabase.auth.getSession();

        if (authError) throw authError;

        const session = 'session' in data ? data.session : null;
        if (!session) {
          throw new Error('No auth session returned.');
        }

        if (!mounted) return;
        setSession(session);
        setUser(session.user);
        setLoading(false);
        if (params.type === 'recovery') {
          router.replace('/auth/reset-password' as any);
        } else {
          routeAfterAuthSession(false);
        }
      } catch (error: any) {
        console.warn('[AuthCallback] Failed to complete auth:', error?.message || error);
        await clearSupabaseAuthStorage().catch(() => {});
        if (!mounted) return;
        setSession(null);
        setUser(null);
        setLoading(false);
        router.replace({
          pathname: '/auth/sign-in',
          params: { error: error?.message || 'Unable to complete sign in.' },
        } as any);
      }
    };

    void completeAuth();

    return () => {
      mounted = false;
    };
  }, [
    params.access_token,
    params.code,
    params.error,
    params.error_code,
    params.error_description,
    params.refresh_token,
    params.type,
    setLoading,
    setSession,
    setUser,
  ]);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.card}>
        <ActivityIndicator color={colors.accent} />
        <Text style={styles.title}>Signing you in</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  card: {
    minWidth: 210,
    minHeight: 136,
    borderRadius: 24,
    backgroundColor: colors.card,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
  },
  title: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '900',
  },
});
