# React + TypeScript + Vite

## User directory

Agents can open **Utilisateurs** at `/agent/users` to browse Odyssey traveler
and agent profiles in read-only mode. The directory searches names and email
addresses and displays 20 profiles per page. Profiles are grouped by type; a
person with both profiles appears in both categories. This is not an Auth0
account directory: accounts without an Odyssey profile are not included.

The `GET /api/users` endpoint requires the `AGENT` role. It accepts `profileType`
(`TRAVELER` or `AGENT`), `search` (up to 120 characters), `page` (zero-based) and
`size` (1–100, default 20). Its response contains `items`, `page`, `size`,
`totalElements` and `totalPages`. Authentication identifiers are not returned.
Traveler profiles include contact details, language and onboarding status;
agent profiles include their availability status.

## Supplier payment simulation

For development/test only, start the API with an active `dev` or `test` Spring
profile and `SUPPLIER_PAYMENT_SIMULATION_ENABLED=true`. The flag defaults to
false. The simulation is also disabled whenever `prod` or `production` is active,
even if the flag is true.

When enabled, traveler supplier-payment buttons open
`/traveler/supplier-payment-simulation/:bookingId` in the same tab instead of the
real supplier URL. This replacement is computed by the API: stored supplier
URLs are not modified. An agent must first set the booking's supplier payment
status to `PAYMENT_REQUIRED`; a real supplier URL is not necessary in this mode.

The page shows persisted booking, accepted quote, traveler and trip details.
It is explicitly labeled as a simulation, takes no card information and transfers
no money. Confirming it really persists `PAID_TO_PROVIDER` on that booking in
the configured database. **Use a development/test database, never production
data.** Confirmation is idempotent, does not confirm the booking itself, does
not pay Odyssey's fees, and does not change the accepted quote's amount.

Both `GET` and `POST /api/bookings/{bookingId}/supplier-payment-simulation`
require the owning `TRAVELER`. Cancelled/inactive bookings cannot be paid.
With simulation disabled the endpoints are absent and real supplier links keep
their existing behavior. Returning to the trip reloads the supplier status and
the existing eligibility rules for Odyssey fees.

## Traveler trip states

The service cards and the trip's next-action banner share the state derived in
`src/helpers/travelerNeedState.ts`. A visible quote's status takes precedence over
the request's searching status: the API can send a quote while its booking
request remains `IN_PROGRESS`. Confirmed, pending, failed and cancelled bookings
retain their priority. A sent proposal therefore displays the review actions
instead of the waiting message, and any proposal ready for review takes priority
in the trip banner.

Run the state regression checks with Node.js supporting TypeScript stripping:

```sh
node --experimental-strip-types src/helpers/travelerTripState.test.ts
```

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```

You can also install [eslint-plugin-react-x](https://npmx.dev/package/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://npmx.dev/package/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```
