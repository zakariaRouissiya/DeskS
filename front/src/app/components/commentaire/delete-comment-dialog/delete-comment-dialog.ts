import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogActions } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-delete-comment-dialog',
  standalone: true,
  templateUrl: './delete-comment-dialog.html',
  styleUrls: ['./delete-comment-dialog.css'],
  imports: [MatDialogActions,MatIconModule]
})
export class DeleteCommentDialog {
  constructor(
    public dialogRef: MatDialogRef<DeleteCommentDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { commentaire: any }
  ) {}

  confirm(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}