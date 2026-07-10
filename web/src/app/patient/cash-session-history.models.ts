export interface CashSessionHistory {
  id: string;
  cashRegisterId: string;
  cashRegisterName: string;
  reportNumber: string;
  openedByUserId: string;
  openedByName: string;
  openedAt: string;
  closedByUserId?: string;
  closedByName?: string;
  closedAt?: string;
  status: 'OPEN' | 'CLOSED';
  openingBalance: number;
  cashReceipts: number;
  chequeReceipts: number;
  transferReceipts: number;
  cashExpenses: number;
  bankDeposits: number;
  expectedCash: number;
  declaredBalance?: number;
  discrepancyAmount?: number;
  discrepancyReason?: string;
  discrepancyResolved: boolean;
}