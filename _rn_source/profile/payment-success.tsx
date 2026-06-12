import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { Button } from '../../src/components/Button';
import { Card } from '../../src/components/Card';
import { Header } from '../../src/components/Header';
import { Screen } from '../../src/components/Screen';
import { colors, spacing, typography } from '../../src/theme';

export default function PaymentSuccessScreen() {
  return (
    <Screen footer={<Button onPress={() => router.replace('/(tabs)/profile')}>Go to Profile</Button>}>
      <Header eyebrow="Payment success" title="Prezzence Pro unlocked" subtitle="Your account is ready for deeper practice." />
      <Card selected>
        <View style={styles.check}><Text style={styles.checkText}>OK</Text></View>
        <Text style={styles.title}>You're ready for deeper practice</Text>
        <Text style={styles.body}>You now have access to more sessions, deeper reports, and shareable score cards.</Text>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  check: { alignItems: 'center', alignSelf: 'center', backgroundColor: colors.success, borderRadius: 36, height: 72, justifyContent: 'center', marginBottom: spacing.lg, width: 72 },
  checkText: { color: colors.background, fontSize: typography.body, fontWeight: '900' },
  title: { color: colors.text, fontSize: typography.h3, fontWeight: '800', textAlign: 'center' },
  body: { color: colors.textMuted, fontSize: typography.body, lineHeight: 24, marginTop: spacing.sm, textAlign: 'center' },
});
