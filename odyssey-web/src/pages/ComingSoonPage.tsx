import { Construction } from "lucide-react";
import { EmptyState } from "../components/ui/State";

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
      <EmptyState
        title="Cette section arrive bientôt"
        description="Elle sera ajoutée lors d’une prochaine étape du développement."
        icon={<Construction size={28} aria-hidden="true" />}
      />
    </div>
  );
}
