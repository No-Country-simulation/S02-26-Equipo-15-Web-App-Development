export const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

function buildApiUrl(path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

export async function apiFetch(path, options) {
  return fetch(buildApiUrl(path), options);
}
