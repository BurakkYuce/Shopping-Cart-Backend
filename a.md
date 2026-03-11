  Ön Koşullar                                                                                                                                                                                                                                                 
                                                                                                                                                                                                                                                            
  - Docker Desktop kurulu ve çalışıyor olmalı                                                                                                                                                                                                                 
  - Dataset dosyaları /Users/burak/Desktop/demo-apps/datasets/output/ klasöründe mevcut olmalı                                                                                                                                                              

  ---
  1. Docker Desktop'ı Başlat

  open -a Docker
  # Menü çubuğundaki balina ikonu duruncaya kadar bekle (~30 sn)

  ---
  2. Proje Klasörüne Git

  cd /Users/burak/Desktop/demo-apps/aad-backend-trial/ecommerce-platform

  ---
  3. Tüm Stack'i Ayağa Kaldır

  docker-compose up -d --build
  İlk seferde ~3-4 dakika sürer (Maven build). Sonraki başlatmalarda ~30 saniye.

  ---
  4. Backend'in Hazır Olmasını Bekle

  docker-compose logs -f backend
  Data loading complete. satırını görünce Ctrl+C ile çık. Backend hazır.

  ---
  5. Kibana Dashboard Kur (Sadece İlk Seferde)

  bash setup_kibana.sh
  bash setup_kibana_alerts.sh

  ---
  6. Sahte Trafik Üret (İsteğe Bağlı)

  bash generate_traffic.sh
  Logların Kibana'ya akmasını görmek için arka planda çalıştır.

  ---
  Servis Adresleri

  ┌───────────────┬───────────────────────────────────────┐
  │    Servis     │                  URL                  │
  ├───────────────┼───────────────────────────────────────┤
  │ Swagger UI    │ http://localhost:8080/swagger-ui.html │
  ├───────────────┼───────────────────────────────────────┤
  │ Kibana        │ http://localhost:5601                 │
  ├───────────────┼───────────────────────────────────────┤
  │ Elasticsearch │ http://localhost:9200                 │
  ├───────────────┼───────────────────────────────────────┤
  │ Backend API   │ http://localhost:8080                 │
  └───────────────┴───────────────────────────────────────┘

  ---
  Test Kullanıcıları

  ┌────────────┬─────────────────────────────┬───────────────┐
  │    Rol     │            Email            │     Şifre     │
  ├────────────┼─────────────────────────────┼───────────────┤
  │ Admin      │ admin@datapulse.com         │ hashed_pw_123 │
  ├────────────┼─────────────────────────────┼───────────────┤
  │ Individual │ user_32d85890@ecommerce.com │ hashed_pw_123 │
  └────────────┴─────────────────────────────┴───────────────┘

  ---
  Kapatmak İçin

  docker-compose down
  Verileri de silmek istersen:
  docker-compose down -v

  ---
  Sorun Giderme

  Port 5000 kullanımda hatası:
  # .env dosyasında zaten 5001'e alındı, sorun çıkmamalı

  eclipse-temurin:17-jre-alpine platform hatası:
  # Dockerfile'da zaten eclipse-temurin:17-jre olarak düzeltildi

  Backend başlamıyor:
  docker-compose logs backend
  # Postgres'in healthy olmasını beklemesi gerekiyor, 30 sn bekle

  Kibana'ya girilemiyor:
  docker-compose up -d kibana
  # Kibana ayrı başlatılmamış olabilir


