from PIL import Image, ImageDraw

# 创建 120x120 的图标
size = 120
img = Image.new('RGB', (size, size), color='#4A90D9')
draw = ImageDraw.Draw(img)

# 绘制哑铃形状
# 基于原 XML 中的设计，缩放到 120x120
# 原设计是 108x108，哑铃位于 27-81 范围内

# 计算缩放比例
scale = size / 108

# 哑铃的坐标（基于 XML 中的路径）
def scale_coords(x, y):
    return (x * scale, y * scale)

# 左侧重量块
left_weight_coords = [
    scale_coords(8, 20),
    scale_coords(8, 34),
    scale_coords(12, 38),
    scale_coords(18, 38),
    scale_coords(18, 16),
    scale_coords(12, 16),
    scale_coords(8, 20)
]

# 右侧重量块
right_weight_coords = [
    scale_coords(36, 16),
    scale_coords(36, 38),
    scale_coords(42, 38),
    scale_coords(46, 34),
    scale_coords(46, 20),
    scale_coords(42, 16),
    scale_coords(36, 16)
]

# 横杆
bar_coords = [
    scale_coords(18, 26),
    scale_coords(36, 26),
    scale_coords(36, 28),
    scale_coords(18, 28)
]

# 握把
handle_coords = [
    scale_coords(20, 22),
    scale_coords(34, 22),
    scale_coords(34, 32),
    scale_coords(20, 32)
]

# 绘制白色哑铃
draw.polygon(left_weight_coords, fill='white')
draw.polygon(right_weight_coords, fill='white')
draw.polygon(bar_coords, fill='white')
draw.polygon(handle_coords, fill='white')

# 保存为 JPG
img.save('app_icon.jpg', 'JPEG', quality=95)
print('图标已生成: app_icon.jpg')