import type { ProviderPaymentStatus } from "../types/booking";

// Shared wording so the financial overview and TravelerNeedCard never
// diverge on how they talk about supplier payments.
export const SUPPLIER_PAYMENT_CTA_LABEL = "Payer directement le fournisseur";
export const SUPPLIER_PAYMENT_DISCLAIMER = "Paiement effectué directement auprès du fournisseur.";

export function getSupplierPaymentLabel(status: ProviderPaymentStatus): string {
  switch (status) {
    case "PAYMENT_REQUIRED":
      return "À régler au fournisseur";
    case "PAID_TO_PROVIDER":
      return "Réglé au fournisseur";
    case "NOT_REQUIRED_YET":
      return "Modalités de paiement à venir";
    case "UNKNOWN":
      return "Informations de paiement en attente";
  }
}
