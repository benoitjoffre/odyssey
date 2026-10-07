export interface CurrentUser {
  roles: string[];
  id?: string;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phoneNumber: string | null;
  whatsappNumber: string | null;
  preferredLanguage: string | null;
  onboardingCompleted: boolean | null;
}
