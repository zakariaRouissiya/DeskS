import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TraitementTicketDialog } from './traitement-ticket-dialog';

describe('TraitementTicketDialog', () => {
  let component: TraitementTicketDialog;
  let fixture: ComponentFixture<TraitementTicketDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TraitementTicketDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TraitementTicketDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
