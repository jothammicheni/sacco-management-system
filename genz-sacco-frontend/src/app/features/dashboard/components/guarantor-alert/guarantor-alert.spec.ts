import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GuarantorAlert } from './guarantor-alert';

describe('GuarantorAlert', () => {
  let component: GuarantorAlert;
  let fixture: ComponentFixture<GuarantorAlert>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GuarantorAlert],
    }).compileComponents();

    fixture = TestBed.createComponent(GuarantorAlert);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
