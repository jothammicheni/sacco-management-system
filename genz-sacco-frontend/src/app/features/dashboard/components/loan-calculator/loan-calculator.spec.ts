import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoanCalculator } from './loan-calculator';

describe('LoanCalculator', () => {
  let component: LoanCalculator;
  let fixture: ComponentFixture<LoanCalculator>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoanCalculator],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanCalculator);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
