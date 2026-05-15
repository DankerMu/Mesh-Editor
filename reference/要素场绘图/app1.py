from flask import Flask, render_template, send_from_directory
import os
from datetime import datetime, timedelta

app = Flask(__name__,
            static_url_path='/static',
            static_folder='static',
            template_folder='templates')

# 确保图片目录结构存在
def create_dirs():
    base_image_path = os.path.join(app.static_folder, 'images')
    regions = ['All', 'NW', 'SW', 'East']
    forecast_types = ['circulation', 'rain']

    # 创建未来7天的目录结构
    today = datetime.now()

    for i in range(7):
        date = today + timedelta(days=i)
        date_str = date.strftime("%Y%m%d")

        for h in ['08', '20']:  # 两个时次
            dir_path = os.path.join(base_image_path, f"{date_str}{h}")

            for forecast_type in forecast_types:
                for region in regions:
                    os.makedirs(os.path.join(dir_path, forecast_type, region), exist_ok=True)

# 应用启动时创建目�?
create_dirs()

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/images/<path:filename>')
def serve_images(filename):
    return send_from_directory(os.path.join(app.static_folder, 'images'), filename)

if __name__ == '__main__':
    app.run(host='192.168.2.80', port=8080)