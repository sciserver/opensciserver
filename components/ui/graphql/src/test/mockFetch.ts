import { vi } from 'vitest';
import { InMemoryLRUCache } from '@apollo/utils.keyvaluecache';

export const TEST_TOKEN = 'test-token';

type Reply = { status?: number; body?: unknown };

// Fake for the fetch a RESTDataSource uses. Replies are queued in call order and every request
// is recorded so tests can assert on the exact URL, method, headers and body sent upstream.
export const createMockFetch = (...replies: Reply[]) => {
  const queue = [...replies];
  const mock = vi.fn(async (_url: unknown, _init?: unknown) => {
    const { status = 200, body = {} } = queue.shift() || {};
    return new Response(JSON.stringify(body), {
      status,
      headers: { 'content-type': 'application/json' }
    });
  });

  const requests = () => mock.mock.calls.map(([url, init]) => {
    const { method, headers, body } = (init || {}) as { method?: string; headers?: Record<string, string>; body?: string };
    return {
      url: String(url),
      method: method || 'GET',
      headers: headers || {},
      body: body ? JSON.parse(body) : undefined
    };
  });

  return { fetch: mock, requests };
};

// Options for constructing a data source against a mocked fetch
export const dataSourceOptions = (fetch: unknown) => ({
  token: TEST_TOKEN,
  cache: new InMemoryLRUCache(),
  fetch
}) as any;
