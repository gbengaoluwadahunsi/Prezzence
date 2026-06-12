import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Stack, router } from 'expo-router';
import * as Linking from 'expo-linking';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import { useState, useEffect } from 'react';
import { clearSupabaseAuthStorage, isRecoverableSupabaseAuthError, supabase } from '../src/lib/supabase';
import { useAuthStore } from '../src/store/authStore';
import { installCrashTracking, trackEvent } from '../src/services/analytics';
import { routeAfterAuthSession } from '../src/services/authNavigation';
import { useNavigationStore } from '../src/store/navigationStore';
import { preloadCoreAssets } from '../src/services/appAssets';
import { installTypographyGuard } from '../src/utils/typographyGuard';

installTypographyGuard();
void SplashScreen.preventAutoHideAsync().catch(() => {});

function getAuthParams(url: string) {
  const [, fragment = ''] = url.split('#');
  const query = url.includes('?') ? url.slice(url.indexOf('?') + 1).split('#')[0] : '';
  return new URLSearchParams(query || fragment);
}

export default function RootLayout() {
  const [queryClient] = useState(() => new QueryClient());
  const { setUser, setSession, setLoading, signOut } = useAuthStore();
  const { setVerificationInProgress } = useNavigationStore();

  useEffect(() => {
    installCrashTracking();
    void preloadCoreAssets();

    const clearBrokenSession = async (reason?: string) => {
      if (reason) {
        console.warn('[RootLayout] Clearing broken session:', reason);
      }
      try {
        await supabase.auth.signOut({ scope: 'local' });
      } catch (signOutError: any) {
        console.warn('[RootLayout] Local sign-out warning:', signOutError?.message || signOutError);
      }
      try {
        await clearSupabaseAuthStorage();
      } catch (storageError: any) {
        console.warn('[RootLayout] Failed to clear invalid auth storage:', storageError?.message || storageError);
      }
      signOut();
    };

    const bootstrapSession = async () => {
      try {
        const { data: { session }, error } = await supabase.auth.getSession();
        if (error || !session) {
          if (error) {
            console.warn('[RootLayout] Session check warning:', error.message);
            if (isRecoverableSupabaseAuthError(error)) {
              await clearBrokenSession(error.message);
            }
          } else {
            signOut();
          }
          trackEvent('app_opened', { authenticated: false });
        } else {
          setSession(session);
          setUser(session.user);
          trackEvent('app_opened', { authenticated: true });
        }
      } catch (error: any) {
        console.warn('[RootLayout] Session check failed:', error?.message || error);
        if (isRecoverableSupabaseAuthError(error)) {
          await clearBrokenSession(error?.message);
        }
        trackEvent('app_opened', { authenticated: false });
      } finally {
        setLoading(false);
        await SplashScreen.hideAsync().catch(() => {});
      }
    };

    void bootstrapSession();

    // Handle deep links
    const handleDeepLink = async (event: { url: string }) => {
      if (__DEV__) {
        console.log('[Deep Link] Incoming URL received');
      }
      const params = getAuthParams(event.url);
      const linkError = params.get('error') || params.get('error_code');
      const linkErrorDescription = params.get('error_description');
      const authCode = params.get('code');
      const accessToken = params.get('access_token');
      const refreshToken = params.get('refresh_token');
      const isPasswordRecovery = event.url.includes('reset-password') || params.get('type') === 'recovery';

      if (linkError || linkErrorDescription) {
        console.warn('[Deep Link] Auth callback rejected:', linkErrorDescription || linkError);
        setVerificationInProgress(false);
        setLoading(false);
        router.replace({
          pathname: '/auth/sign-in',
          params: {
            signup: 'true',
            error: linkErrorDescription || 'This verification link is invalid or expired. Please request a new email.',
          },
        } as any);
        return;
      }

      if (authCode || (accessToken && refreshToken)) {
        try {
          setVerificationInProgress(true);
          const { data, error } = authCode
            ? await supabase.auth.exchangeCodeForSession(authCode)
            : await supabase.auth.setSession({
                access_token: accessToken ?? '',
                refresh_token: refreshToken ?? '',
              });

          if (error) throw error;
          if (data.session) {
            setSession(data.session);
            setUser(data.session.user);
            setLoading(false);
            if (isPasswordRecovery) {
              router.replace('/auth/reset-password' as any);
            } else {
              routeAfterAuthSession(false);
            }
          }
        } catch (error) {
          console.error('[Deep Link] Auth callback failed:', error);
          setVerificationInProgress(false);
          router.replace('/auth/sign-in');
        }
        return;
      }

      if (event.url.includes('verify')) {
        setVerificationInProgress(true);
        setLoading(false);
        const { data: { session } } = await supabase.auth.getSession();
        if (session) {
          setSession(session);
          setUser(session.user);
          if (__DEV__) {
            console.log('[Deep Link] Verification flow detected with active session');
          }
          router.replace('/verify' as any);
        } else {
          if (__DEV__) {
            console.log('[Deep Link] Verification flow detected without active session; routing to sign in');
          }
          setVerificationInProgress(false);
          router.replace({ pathname: '/auth/sign-in', params: { signup: 'true', verified: 'true' } } as any);
        }
      }
    };

    const subscription_link = Linking.addEventListener('url', handleDeepLink);

    // Check if app was opened from a link
    Linking.getInitialURL().then((url) => {
      if (url) handleDeepLink({ url });
    });

    // Listen for auth changes
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      const verificationInProgress = useNavigationStore.getState().verificationInProgress;
      if (__DEV__) {
        console.log('[Auth State Change] Event:', _event, 'verifyInProgress:', verificationInProgress);
      }
      setSession(session);
      setUser(session?.user ?? null);
      setLoading(false);
    });

    return () => {
      subscription.unsubscribe();
      subscription_link.remove();
    };
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          contentStyle: { backgroundColor: '#0A0A0F' },
          headerShown: false,
        }}
      >
        <Stack.Screen name="index" />
        <Stack.Screen name="verify" />
        <Stack.Screen name="auth" />
        <Stack.Screen name="onboarding" />
        <Stack.Screen name="interview" />
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="sessions" />
        <Stack.Screen name="profile" />
        <Stack.Screen name="settings" />
        <Stack.Screen name="errors" />
        <Stack.Screen name="legal" />
        <Stack.Screen name="feedback" />
        <Stack.Screen name="(modals)" options={{ presentation: 'modal' }} />
      </Stack>
    </QueryClientProvider>
  );
}
