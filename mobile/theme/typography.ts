export const fonts = {
  heading: 'Oswald_700Bold',
  headingMedium: 'Oswald_500Medium',
  body: undefined,
} as const;

// Generous line-height and letter-spacing throughout — the single biggest lever
// for making a dense app feel calm and "modern minimalist" rather than boxed-in.
export const typography = {
  display: { fontFamily: fonts.heading, fontSize: 34, lineHeight: 38, letterSpacing: 0.4 },
  h1: { fontFamily: fonts.heading, fontSize: 28, lineHeight: 32, letterSpacing: 0.4 },
  h2: { fontFamily: fonts.heading, fontSize: 20, lineHeight: 24, letterSpacing: 0.4 },
  h3: { fontFamily: fonts.headingMedium, fontSize: 16, lineHeight: 20, letterSpacing: 0.3 },
  label: { fontFamily: fonts.headingMedium, fontSize: 13, lineHeight: 16, letterSpacing: 0.6 },
  body: { fontSize: 15, lineHeight: 22 },
  bodyMuted: { fontSize: 13, lineHeight: 19, letterSpacing: 0.1 },
  caption: { fontSize: 12, lineHeight: 16 },
} as const;
