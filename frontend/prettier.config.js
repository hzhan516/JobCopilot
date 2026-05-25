/** @type {import("prettier").Config} */
export default {
  // Line length (match Java backend style)
  printWidth: 100,

  // Use 2 spaces (frontend standard; backend Java uses 4)
  tabWidth: 2,

  // Use spaces, not tabs
  useTabs: false,

  // Always use semicolons
  semi: true,

  // Use single quotes for strings
  singleQuote: true,

  // Quote props only when necessary
  quoteProps: 'as-needed',

  // Use double quotes in JSX (React convention)
  jsxSingleQuote: false,

  // Trailing commas (ES5 compatible)
  trailingComma: 'es5',

  // Bracket spacing: { foo: bar }
  bracketSpacing: true,

  // JSX brackets on same line
  bracketSameLine: false,

  // Arrow function parens: (x) => x
  arrowParens: 'always',

  // Sort imports (requires @trivago/prettier-plugin-sort-imports or similar)
  // For now, rely on ESLint import/order

  // End of line: LF (Unix-style)
  endOfLine: 'lf',

  // Plugins
  plugins: [],
};
