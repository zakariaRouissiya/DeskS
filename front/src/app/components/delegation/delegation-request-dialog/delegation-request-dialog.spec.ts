import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DelegationRequestDialog } from './delegation-request-dialog';

describe('DelegationRequestDialog', () => {
  let component: DelegationRequestDialog;
  let fixture: ComponentFixture<DelegationRequestDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DelegationRequestDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DelegationRequestDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
