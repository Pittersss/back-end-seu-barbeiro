export const fonts = {
  heading: 'Oswald_700Bold',
  headingMedium: 'Oswald_500Medium',
  body: undefined,
} as const;

export const typography = {
  h1: { fontFamily: fonts.heading, fontSize: 28, letterSpacing: 0.4 },
  h2: { fontFamily: fonts.heading, fontSize: 20, letterSpacing: 0.4 },
  h3: { fontFamily: fonts.headingMedium, fontSize: 16, letterSpacing: 0.3 },
  label: { fontFamily: fonts.headingMedium, fontSize: 13, letterSpacing: 0.2 },
  body: { fontSize: 15 },
  bodyMuted: { fontSize: 13, letterSpacing: 0.1 },
  caption: { fontSize: 12 },
} as const;
