import { CreditCard } from "lucide-react";
import { Link } from "react-router-dom";
import { SUPPLIER_PAYMENT_CTA_LABEL, SUPPLIER_PAYMENT_DISCLAIMER } from "../helpers/supplierPayment";

export function SupplierPaymentLink({ url, className }: { url: string; className: string }) {
  const simulation = url.startsWith("/traveler/supplier-payment-simulation/");
  const content = <><CreditCard size={18} aria-hidden="true" /> {simulation ? "Simuler le paiement fournisseur" : SUPPLIER_PAYMENT_CTA_LABEL}</>;

  return (
    <>
      {simulation ? (
        <Link className={className} to={url}>{content}</Link>
      ) : (
        <a className={className} href={url} target="_blank" rel="noopener noreferrer">{content}</a>
      )}
      <p className="traveler-provider-payment-hint">
        {simulation ? "Simulation de test : aucun débit. Le statut du dossier sera modifié." : SUPPLIER_PAYMENT_DISCLAIMER}
      </p>
    </>
  );
}
