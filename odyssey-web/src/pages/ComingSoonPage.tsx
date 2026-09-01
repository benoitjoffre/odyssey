import { Construction } from "lucide-react";

interface ComingSoonPageProps {
  title: string;
}

export function ComingSoonPage({ title }: ComingSoonPageProps) {
  return (
    <div className="page-stack">
      <section className="page-heading compact">
        <div>
          <span className="eyebrow">Espace Agent</span>
          <h1>{title}</h1>
        </div>
      </section>
      <div className="state-panel">
        <Construction size={28} aria-hidden="true" />
        <strong>Cette section arrive bientôt</strong>
        <p>Elle sera ajoutée lors d’une prochaine étape du développement.</p>
      </div>
    </div>
  );
}
