import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Stores } from './stores';

describe('Stores', () => {
  let component: Stores;
  let fixture: ComponentFixture<Stores>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Stores]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Stores);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
