import { Component, effect, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TodoService } from '../../services/app.todo-service';
import { TodoList } from '../todo-list/todo-list';

@Component({
  selector: 'app-todo',
  imports: [ReactiveFormsModule, TodoList],
  templateUrl: './todo.html',
  styleUrl: './todo.scss',
})
export class TodoComponent {
  title = signal('Task Manager');
  taskControl = new FormControl('', { nonNullable: true });
  todoService = inject(TodoService);

  todos = this.todoService.todos;
  remainingCount = this.todoService.remainingCount;
  completedCount = this.todoService.completedCount;

  constructor() {
    effect(() => {
      console.log('Changed', this.todos().length);
    });
  }

  addTask() {
    const value = this.taskControl.value.trim();
    if (value) {
      this.todoService.addTodo(value);
      this.taskControl.reset();
    }
  }

}

