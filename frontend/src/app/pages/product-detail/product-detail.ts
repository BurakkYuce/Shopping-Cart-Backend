import { Component, OnInit} from '@angular/core';
import { Product as ProductService } from '../../core/services/product';
import { Product as ProductModel } from '../../models/product';
import { ActivatedRoute,RouterLink } from '@angular/router';


@Component({
  selector: 'app-product-detail',
  imports: [RouterLink],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetail implements OnInit {
  product: ProductModel | undefined;

  constructor(private route: ActivatedRoute, private productService: ProductService) {}

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
  
    const id = Number(idParam);

    this.product = this.productService.getProductById(id);
  }
}
