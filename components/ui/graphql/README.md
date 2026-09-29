# 👩‍💻 What's GraphQL?

GraphQL is a query language for APIs that has quickly gained popularity over the last few years as an alternative to REST.

[Click here](https://graphql.org/learn/) or a quick guide on how it works.


## Getting Started

First, run the development server:

```bash
yarn dev
```

Open [http://localhost:4000/playground](http://localhost:4000/playground) with your browser to test queries and mutations.

For that you'll need a token from the Scierver Dashboard. You need to paste it in the `HTTP HEADERS` tab on the bottom left side of the page (see image below). 

<img width="347" alt="image" src="https://user-images.githubusercontent.com/27710492/211905445-920d2dfc-1e5f-43b0-b651-dfe4febccf77.png">


## Making changes

When making changes to the `types` folder, you'll need to run 

```bash
yarn generate
```

for the server to recognize the changes.


## Tests

Tests use [Vitest](https://vitest.dev) and live next to the code as `*.test.ts`.

```bash
yarn test          # run once
yarn test:watch    # re-run on change
yarn test:types    # type-check the tests (they are excluded from the app build)
```

The fileservice and RACM are never called: each test injects a fake `fetch` (see `src/test/mockFetch.ts`) into the data source and asserts on the exact requests sent upstream. `vitest.config.mts` sets fake service URLs, so a local `.env` pointing at real services is safe to have.
