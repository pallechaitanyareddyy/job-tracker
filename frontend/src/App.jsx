import { useEffect, useState } from 'react';
import { fetchHealth } from './api/health.js';

export default function App() {
  const [state, setState] = useState({ loading: true, data: null, error: null });

  useEffect(() => {
    fetchHealth()
      .then((data) => setState({ loading: false, data, error: null }))
      .catch((error) => setState({ loading: false, data: null, error: error.message }));
  }, []);

  return (
    <main className="page">
      <h1>Job Tracker</h1>
      <p className="subtitle">Slice 1: checking that the frontend can reach the backend.</p>

      <section className="card" aria-live="polite">
        {state.loading && <p>Contacting the backend...</p>}

        {state.data && (
          <>
            <p className="status up">Backend status: {state.data.status}</p>
            <p>Service: {state.data.service}</p>
            <p>Server time: {state.data.time}</p>
          </>
        )}

        {state.error && (
          <>
            <p className="status down">Backend not reachable</p>
            <p>{state.error}</p>
            <p>Is the Spring Boot app running on port 8080?</p>
          </>
        )}
      </section>
    </main>
  );
}
