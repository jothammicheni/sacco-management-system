import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActiveLoan } from './active-loan';

describe('ActiveLoan', () => {
  let component: ActiveLoan;
  let fixture: ComponentFixture<ActiveLoan>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActiveLoan],
    }).compileComponents();

    fixture = TestBed.createComponent(ActiveLoan);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
