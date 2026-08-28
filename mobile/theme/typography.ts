export const fonts = {
  heading: 'Oswald_700Bold',
  headingMedium: 'Oswald_500Medium',
  body: undefined,
} as const;

export const typography = {
  h1: { fontFamily: fonts.heading, fontSize: 28, letterSpacing: 0.5 },
  h2: { fontFamily: fonts.heading, fontSize: 20, letterSpacing: 0.5 },
  label: { fontFamily: fonts.headingMedium, fontSize: 13, letterSpacing: 0.3 },
  body: { fontSize: 15 },
  caption: { fontSize: 12 },
} as const;
