import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Button } from '../../src/components/Button';
import { Card } from '../../src/components/Card';
import { Header } from '../../src/components/Header';
import { Screen } from '../../src/components/Screen';
import { colors, spacing, typography } from '../../src/theme';

const settings = ['Reduce motion', 'Download over Wi-Fi only'];

export default function AppSettingsScreen() {
  const [enabled, setEnabled] = useState(['Download over Wi-Fi only']);

  const toggle = (setting: string) => {
    setEnabled((current) => current.includes(setting) ? current.filter((item) => item !== setting) : [...current, setting]);
  };

  return (
    <Screen footer={<Button onPress={() => router.back()}>Save Settings</Button>}>
      <Header eyebrow="App settings" title="Personalize Prezzence" subtitle="Manage accessibility and app settings." />
      <View style={styles.list}>
        {settings.map((setting) => (
          <Card key={setting} selected={enabled.includes(setting)} onPress={() => toggle(setting)}>
            <Text style={styles.setting}>{setting}</Text>
          </Card>
        ))}
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({ list: { gap: spacing.md }, setting: { color: colors.text, fontSize: typography.body, fontWeight: '800' } });
