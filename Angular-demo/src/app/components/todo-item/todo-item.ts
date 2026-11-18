import { Component, EventEmitter, Input, input, Output, output } from '@angular/core';
import { TODO } from '../../models/model';

@Component({
  imports: [],
  selector: 'app-todo-item',
  styleUrl: './todo-item.scss',
  templateUrl: './todo-item.html',
})
export class TodoItem {
  // @Input() name: string = '';
  // @Output() myEvent: EventEmitter<string> = new EventEmitter<string>();
  todo = input.required<TODO>();
  deleteTaskEvent = output<TODO>();
  updateTodoEvent = output<TODO>();

  deleteTask() {
    this.deleteTaskEvent.emit(this.todo());
  }

  updateTodo() {
    this.updateTodoEvent.emit(this.todo());
  }
}
