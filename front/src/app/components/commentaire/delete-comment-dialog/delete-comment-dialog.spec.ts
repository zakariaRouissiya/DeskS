import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeleteCommentDialog } from './delete-comment-dialog';

describe('DeleteCommentDialog', () => {
  let component: DeleteCommentDialog;
  let fixture: ComponentFixture<DeleteCommentDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeleteCommentDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeleteCommentDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
