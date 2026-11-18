import { computed, inject, Injectable, signal } from '@angular/core';
import { TODO } from '../models/model';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TodoService {
  private http = inject(HttpClient);
  // 1. Private writable signal (internal state)
  private readonly _todos = signal<TODO[]>([
    { id: crypto.randomUUID(), title: 'Learn Angular signals', completed: true },
    { id: crypto.randomUUID(), title: 'Build modern UI components', completed: false },
    { id: crypto.randomUUID(), title: 'Master reactive state flow', completed: false },
  ]);

  // 2. Public read-only signal (exposed to components)
  readonly todos = this._todos.asReadonly();

  readonly remainingCount = computed(() => this.todos().filter((t) => !t.completed).length);
  readonly completedCount = computed(() => this.todos().filter((t) => t.completed).length);

  addTodo(title: string) {
    const newTodo: TODO = {
      id: crypto.randomUUID(),
      title: title,
      completed: false,
    };
    this._todos.update((todos) => [...todos, newTodo]);
  }

  toggleTodo(id: string) {
    this._todos.update((todos) =>
      todos.map((todo) => (todo.id === id ? { ...todo, completed: !todo.completed } : todo))
    );
  }

  deleteTodo(id: string) {
    this._todos.update((todos) => todos.filter((t) => t.id !== id));
  }


  getAllPosts(): Observable<any> {
    return this.http.get("https://jsonplaceholder.typicode.com/posts");
  }
}
