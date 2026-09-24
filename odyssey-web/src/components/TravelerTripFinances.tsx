import type { ReactNode } from "react";
import { AlertTriangle, Check, Circle, Clock3, CreditCard, LoaderCircle } from "lucide-react";
import { getSupplierPaymentLabel, SUPPLIER_PAYMENT_CTA_LABEL, SUPPLIER_PAYMENT_DISCLAIMER } from "../helpers/supplierPayment";
import type { ProviderPaymentStatus } from "../types/booking";
import type { PaymentStatus } from "../types/payment";

export interface SupplierServiceAmount {
  needId: number;
  icon: ReactNode;
  label: string;
  providerPrice: number;
  currency: string;
  providerPaymentStatus: ProviderPaymentStatus;
  providerPaymentUrl: string | null;
}

export interface TravelerTripFinancesProps {
  assistanceFee: number;
  /** Odyssey assistance fee payment status, or null when no payment has been started yet. */
  paymentStatus: PaymentStatus | null;
  /** Mirrors backend TripDetail.assistanceFeePayable. Never re-derived independently. */
  tripEligibleForPayment: boolean;
  checkoutLoading: boolean;
  checkoutError: string | null;
  onCheckout: () => void;
  supplierServices: SupplierServiceAmount[];
}

function formatPrice(price: number, currency: string) {
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency,
    maximumFractionDigits: 2,
  }).format(price);
}

function getSupplierTotalsByCurrency(services: SupplierServiceAmount[]) {
  return services.reduce<Map<string, number>>((totals, service) => {
    totals.set(service.currency, (totals.get(service.currency) ?? 0) + service.providerPrice);
    return totals;
  }, new Map());
}

export function TravelerTripFinances({
  assistanceFee,
  paymentStatus,
  tripEligibleForPayment,
  checkoutLoading,
  checkoutError,
  onCheckout,
  supplierServices,
}: TravelerTripFinancesProps) {
  const supplierTotals = getSupplierTotalsByCurrency(supplierServices);
  const supplierCurrencies = [...supplierTotals.keys()];

  return (
    <section id="trip-financial-summary" className="trip-finances" aria-labelledby="trip-finances-title">
      <div className="section-heading">
        <div>
          <h2 id="trip-finances-title">Finances de votre voyage</h2>
        </div>
      </div>

      <div className="trip-finances-blocks">
        <div className="trip-finance-block">
          <h3>Frais d'accompagnement Odyssey</h3>
          <p className="trip-finance-block-description">Ce montant couvre l'accompagnement et l'organisation de votre voyage.</p>

          {assistanceFee === 0 ? (
            <p className="trip-finance-status trip-finance-status--muted">
              <Circle size={16} aria-hidden="true" /> Aucun frais d'accompagnement à régler
            </p>
          ) : paymentStatus === "PAID" ? (
            <p className="trip-finance-status trip-finance-status--success">
              <Check size={16} aria-hidden="true" /> Frais Odyssey réglés
            </p>
          ) : paymentStatus === "PENDING" ? (
            <>
              <p className="trip-finance-status trip-finance-status--progress">
                <Clock3 size={16} aria-hidden="true" /> Paiement en cours
              </p>
              <button type="button" className="primary-button" disabled={checkoutLoading} onClick={onCheckout}>
                {checkoutLoading ? <LoaderCircle className="rotating" size={16} /> : <CreditCard size={16} />}
                {checkoutLoading ? "Redirection vers Stripe…" : "Reprendre le paiement"}
              </button>
            </>
          ) : paymentStatus === "FAILED" ? (
            <>
              <p className="trip-finance-status trip-finance-status--error">
                <AlertTriangle size={16} aria-hidden="true" /> Le paiement n'a pas abouti
              </p>
              {tripEligibleForPayment && (
                <button type="button" className="primary-button" disabled={checkoutLoading} onClick={onCheckout}>
                  {checkoutLoading ? <LoaderCircle className="rotating" size={16} /> : <CreditCard size={16} />}
                  {checkoutLoading ? "Redirection vers Stripe…" : "Réessayer le paiement"}
                </button>
              )}
            </>
          ) : tripEligibleForPayment ? (
            <button type="button" className="primary-button" disabled={checkoutLoading} onClick={onCheckout}>
              {checkoutLoading ? <LoaderCircle className="rotating" size={16} /> : <CreditCard size={16} />}
              {checkoutLoading ? "Redirection vers Stripe…" : `Régler mes frais Odyssey — ${formatPrice(assistanceFee, "EUR")}`}
            </button>
          ) : (
            <p className="trip-finance-status trip-finance-status--muted">
              Vous pourrez régler vos frais d'accompagnement Odyssey lorsque vos réservations auprès des fournisseurs seront finalisées.
            </p>
          )}

          {checkoutError && (
            <p className="trip-financial-error" role="alert">
              {checkoutError}
            </p>
          )}
        </div>

        <div className="trip-finance-block">
          <h3>Prestations de votre voyage</h3>
          <p className="trip-finance-block-description">Ces montants sont réglés directement aux fournisseurs.</p>

          {supplierServices.length === 0 ? (
            <p className="trip-finance-status trip-finance-status--muted">Aucune offre fournisseur retenue pour le moment.</p>
          ) : (
            <ul className="trip-finance-supplier-list">
              {supplierServices.map((service) => (
                <li key={service.needId} className="trip-finance-supplier-item">
                  <div className="trip-finance-supplier-item-header">
                    <span aria-hidden="true">{service.icon}</span>
                    <strong>{service.label}</strong>
                    <span className="trip-finance-supplier-item-price">{formatPrice(service.providerPrice, service.currency)}</span>
                  </div>
                  <p className="trip-finance-supplier-item-status">{getSupplierPaymentLabel(service.providerPaymentStatus)}</p>
                  {service.providerPaymentStatus === "PAYMENT_REQUIRED" && service.providerPaymentUrl && (
                    <div className="trip-finance-supplier-cta">
                      <a className="primary-button" href={service.providerPaymentUrl} target="_blank" rel="noopener noreferrer">
                        <CreditCard size={16} aria-hidden="true" /> {SUPPLIER_PAYMENT_CTA_LABEL}
                      </a>
                      <p className="traveler-provider-payment-hint">{SUPPLIER_PAYMENT_DISCLAIMER}</p>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}

          {supplierServices.length > 1 && (
            <p className="trip-finance-supplier-subtotal">
              Sous-total fournisseurs :{" "}
              {supplierCurrencies.map((currency, index) => (
                <span key={currency}>
                  {index > 0 ? " + " : ""}
                  {formatPrice(supplierTotals.get(currency) ?? 0, currency)}
                </span>
              ))}
            </p>
          )}
        </div>
      </div>

      <p className="trip-finances-explanation">
        Odyssey facture uniquement son accompagnement. Les prestations de votre voyage sont réglées directement auprès des fournisseurs.
      </p>
    </section>
  );
}
