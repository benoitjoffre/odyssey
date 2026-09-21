export interface CurrentUser {
  roles: string[];
  id?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  onboardingCompleted: boolean | null;
}
