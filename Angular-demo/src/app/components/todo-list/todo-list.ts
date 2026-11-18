import { Component, inject, Input, OnInit } from '@angular/core';
import { TodoItem } from '../todo-item/todo-item';
import { TODO } from '../../models/model';
import { TodoService } from '../../services/app.todo-service';

@Component({
  imports: [TodoItem],
  selector: 'app-todo-list',
  styleUrl: './todo-list.scss',
  templateUrl: './todo-list.html',
})
export class TodoList implements OnInit {
  ngOnInit(): void {
    this.todoService.getAllPosts().subscribe((posts) => {
      console.log(posts);
    });
  }
  todoService = inject(TodoService);
  @Input() todos = this.todoService.todos;

  deleteTask(todo: TODO) {
    this.todoService.deleteTodo(todo.id);
  }

  updateTodo(todo: TODO) {
    this.todoService.toggleTodo(todo.id);
  }


}
