#!/usr/bin/env python3
"""
BaseFit APK Interactive Packaging Script
Cross-platform: Windows, macOS, Linux
Compatible with: cmd.exe, PowerShell, Terminal.app, bash, zsh, etc.
"""

import os
import sys
import subprocess
import shutil
import re
from pathlib import Path
from datetime import datetime

SCRIPT_VERSION = "1.0.0"
PROJECT_ROOT = Path(__file__).parent.resolve()
GRADLE_WRAPPER = PROJECT_ROOT / "gradlew.bat" if sys.platform == "win32" else PROJECT_ROOT / "gradlew"
DEFAULT_OUTPUT_DIR = PROJECT_ROOT / "output"
VERSION_FILE = PROJECT_ROOT / ".apk_version"
BUILD_TYPE = "release"


class Colors:
    HEADER = "\033[95m"
    OKBLUE = "\033[94m"
    OKCYAN = "\033[96m"
    OKGREEN = "\033[92m"
    WARNING = "\033[93m"
    FAIL = "\033[91m"
    ENDC = "\033[0m"
    BOLD = "\033[1m"


def is_windows():
    return sys.platform == "win32"


def supports_ansi():
    if is_windows():
        return os.environ.get("ANSICON") or os.environ.get("WT_SESSION") or os.environ.get("TERM_PROGRAM") == "vscode"
    return True


def color(text, color_code):
    if supports_ansi():
        return f"{color_code}{text}{Colors.ENDC}"
    return text


def clear_screen():
    os.system("cls" if is_windows() else "clear")


def show_header():
    clear_screen()
    print(color("=" * 49, Colors.OKCYAN))
    print(color("    BaseFit APK Interactive Packaging Tool", Colors.OKCYAN))
    print(color(f"              Version {SCRIPT_VERSION}", Colors.OKCYAN))
    print(color("=" * 49, Colors.OKCYAN))
    print()


def show_step(number, description):
    print()
    print(color(f"[STEP {number}/6] {description}", Colors.WARNING))
    print(color("-" * 49, Colors.OKCYAN))


def get_input(prompt, default=None, validator=None):
    if default:
        prompt_display = f"{prompt} [{default}]"
    else:
        prompt_display = prompt

    while True:
        try:
            user_input = input(f"{prompt_display}: ").strip()
        except (KeyboardInterrupt, EOFError):
            print("\n已取消打包操作")
            sys.exit(0)

        if not user_input:
            if default is not None:
                return default
            elif default is None and validator is None:
                return user_input
            print(color("[错误] 此项为必填项", Colors.FAIL))
            continue

        if validator:
            is_valid, error_msg = validator(user_input)
            if is_valid:
                return user_input
            print(color(f"[错误] {error_msg}", Colors.FAIL))
        else:
            return user_input


def get_version():
    print()
    user_input = get_input("请输入版本号 (例如: 1.0.0)", default=None)

    if not user_input:
        if VERSION_FILE.exists():
            last_version = VERSION_FILE.read_text().strip()
            match = re.match(r"(\d+)\.(\d+)\.(\d+)", last_version)
            if match:
                major, minor, patch = match.groups()
                new_patch = int(patch) + 1
                version = f"{major}.{minor}.{new_patch}"
                print(color(f"[自动] 使用自增版本号: {version}", Colors.OKGREEN))
                return version

        version = "1.0.0"
        print(color(f"[自动] 使用自增版本号: {version}", Colors.OKGREEN))
        return version
    else:
        print(color(f"[确认] 使用指定版本号: {user_input}", Colors.OKGREEN))
        return user_input


def get_date_option():
    print()
    options = {"Y": True, "y": True, "N": False, "n": False, "": False}
    user_input = get_input("是否在文件名中包含日期? (Y/N)", default="N")

    if user_input in ("Y", "y"):
        build_date = datetime.now().strftime("%Y%m%d")
        print(color(f"[确认] 文件名将包含日期: {build_date}", Colors.OKGREEN))
        return True, build_date
    else:
        print(color("[默认] 文件名不包含日期", Colors.OKGREEN))
        return False, None


def get_output_directory():
    print()
    user_input = get_input(f"请输入APK输出目录", default=str(DEFAULT_OUTPUT_DIR))

    output_dir = Path(user_input)
    if not output_dir.exists():
        print(color(f"[警告] 目录不存在，正在创建: {output_dir}", Colors.WARNING))
        try:
            output_dir.mkdir(parents=True, exist_ok=True)
        except Exception as e:
            print(color(f"[错误] 无法创建输出目录: {e}", Colors.FAIL))
            return get_output_directory()

    print(color(f"[确认] APK将输出到: {output_dir}", Colors.OKGREEN))
    return output_dir


def get_build_type():
    print()
    print("请选择构建类型:")
    print("  1) Release (发布版本)")
    print("  2) Debug (调试版本)")

    user_input = get_input("请选择", default="1")

    if user_input == "1" or not user_input:
        build_type = "release"
    elif user_input == "2":
        build_type = "debug"
    else:
        print(color("[错误] 无效选择，请输入 1 或 2", Colors.FAIL))
        return get_build_type()

    print(color(f"[确认] 构建类型: {build_type}", Colors.OKGREEN))
    return build_type


def show_summary(build_type, version_name, include_date, build_date, output_dir):
    print()
    print(color("=" * 49, Colors.WARNING))
    print(color("           配置信息确认", Colors.WARNING))
    print(color("=" * 49, Colors.WARNING))
    print()
    print(f"  项目路径:    {PROJECT_ROOT}")
    print(f"  构建类型:    {build_type}")
    print(f"  版本号:      {version_name}")
    print(f"  包含日期:    {build_date if include_date else '无'}")
    print(f"  输出目录:    {output_dir}")
    print()
    print(color("=" * 49, Colors.WARNING))


def confirm_build():
    print()
    user_input = get_input("确认开始打包? (Y/N)", default="N")

    if user_input in ("N", "n", ""):
        print(color("已取消打包操作", Colors.WARNING))
        sys.exit(0)
    elif user_input not in ("Y", "y"):
        print(color("[错误] 请输入 Y 或 N", Colors.FAIL))
        return confirm_build()


def test_environment():
    if not GRADLE_WRAPPER.exists():
        gradle_path = str(GRADLE_WRAPPER) if is_windows() else str(GRADLE_WRAPPER) + " (or gradlew)"
        print(color(f"[错误] Gradle Wrapper 未找到: {gradle_path}", Colors.FAIL))
        return False
    print(color("[OK] Gradle Wrapper 存在", Colors.OKGREEN))

    build_file = PROJECT_ROOT / "build.gradle.kts"
    if not build_file.exists():
        print(color("[错误] build.gradle.kts 未找到", Colors.FAIL))
        return False
    print(color("[OK] build.gradle.kts 存在", Colors.OKGREEN))

    return True


def invoke_build(build_type):
    print(color(f"正在执行: gradlew clean assemble{build_type}", Colors.OKCYAN))

    cmd = [str(GRADLE_WRAPPER), "clean", f"assemble{build_type}", "--stacktrace"]

    try:
        result = subprocess.run(
            cmd,
            cwd=str(PROJECT_ROOT),
            shell=is_windows()
        )

        if result.returncode != 0:
            print(color("[错误] Gradle 构建失败!", Colors.FAIL))
            return False

        print(color("[OK] Gradle 构建成功", Colors.OKGREEN))
        return True

    except FileNotFoundError:
        if not is_windows():
            print(color("[错误] gradlew 未找到，尝试添加执行权限...", Colors.WARNING))
            try:
                os.chmod(str(GRADLE_WRAPPER), 0o755)
                result = subprocess.run(
                    ["./gradlew", "clean", f"assemble{build_type}", "--stacktrace"],
                    cwd=str(PROJECT_ROOT)
                )
                if result.returncode != 0:
                    print(color("[错误] Gradle 构建失败!", Colors.FAIL))
                    return False
                print(color("[OK] Gradle 构建成功", Colors.OKGREEN))
                return True
            except Exception as e:
                print(color(f"[错误] 执行构建失败: {e}", Colors.FAIL))
                return False
        print(color("[错误] gradlew.bat 未找到", Colors.FAIL))
        return False

    except Exception as e:
        print(color(f"[错误] Gradle 构建失败: {e}", Colors.FAIL))
        return False


def find_apk(build_type):
    apk_path = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / build_type / f"app-{build_type}.apk"

    if apk_path.exists():
        print(color(f"[OK] 找到APK: {apk_path}", Colors.OKGREEN))
        return apk_path

    print(color("[警告] 未找到标准路径的APK，正在搜索...", Colors.WARNING))

    apk_dir = PROJECT_ROOT / "app" / "build" / "outputs" / "apk"
    if apk_dir.exists():
        apks = list(apk_dir.rglob("*.apk"))
        if apks:
            found_apk = apks[0]
            print(color(f"[OK] 找到APK: {found_apk}", Colors.OKGREEN))
            return found_apk

    print(color("[错误] 无法找到生成的APK文件", Colors.FAIL))
    return None


def generate_output_filename(version_name, include_date, build_date):
    filename = f"basefit-{version_name}"
    if include_date and build_date:
        filename = f"{filename}-{build_date}"
    return f"{filename}.apk"


def copy_apk_to_output(apk_source, output_dir, output_filename):
    final_apk = output_dir / output_filename

    try:
        shutil.copy2(apk_source, final_apk)
        print(color(f"[OK] APK已复制到: {final_apk}", Colors.OKGREEN))
    except Exception as e:
        print(color(f"[错误] 复制APK失败: {e}", Colors.FAIL))
        return False

    try:
        VERSION_FILE.write_text(apk_source)
    except Exception:
        pass

    try:
        file_size = final_apk.stat().st_size
    except Exception:
        file_size = 0

    return True, file_size


def show_success(output_filename, output_dir, version_name, file_size):
    print()
    print(color("=" * 49, Colors.OKGREEN))
    print(color("       APK 打包完成!", Colors.OKGREEN))
    print(color("=" * 49, Colors.OKGREEN))
    print()
    print(f"  文件名:   {output_filename}")
    print(f"  路径:     {output_dir / output_filename}")
    print(f"  版本:     {version_name}")
    print(f"  大小:     {file_size} bytes")
    print()
    print(color(f"提示: APK版本号 {version_name} 已保存，下次打包时可自动自增", Colors.OKCYAN))
    print()
    try:
        input("按 Enter 键退出...")
    except (KeyboardInterrupt, EOFError):
        pass


def main():
    show_header()

    show_step(1, "收集构建参数")
    version_name = get_version()
    include_date, build_date = get_date_option()
    output_dir = get_output_directory()
    build_type = get_build_type()

    show_summary(build_type, version_name, include_date, build_date, output_dir)
    confirm_build()

    if not test_environment():
        input("\n按 Enter 键退出...")
        sys.exit(1)

    show_step(2, "验证构建环境")
    if not test_environment():
        input("\n按 Enter 键退出...")
        sys.exit(1)

    show_step(3, "执行构建命令")
    if not invoke_build(build_type):
        input("\n按 Enter 键退出...")
        sys.exit(1)

    show_step(4, "处理APK文件")
    apk_source = find_apk(build_type)
    if not apk_source:
        input("\n按 Enter 键退出...")
        sys.exit(1)

    output_filename = generate_output_filename(version_name, include_date, build_date)
    VERSION_FILE.write_text(version_name)

    show_step(5, "复制APK到输出目录")
    result = copy_apk_to_output(apk_source, output_dir, output_filename)
    if not result:
        input("\n按 Enter 键退出...")
        sys.exit(1)

    _, file_size = result

    show_step(6, "完成")
    show_success(output_filename, output_dir, version_name, file_size)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] in ("-h", "--help"):
        print("""
使用方法:
  python Build-Apk.py
  ./Build-Apk.py (Linux/macOS with execute permission)

功能说明:
  1. 交互式收集构建参数（版本号、日期、输出目录、构建类型）
  2. 自动验证构建环境
  3. 执行 Gradle 构建
  4. 查找并重命名 APK 文件
  5. 复制到指定输出目录

默认值:
  - 版本号: 自动自增 (首次为 1.0.0)
  - 日期: 不包含
  - 输出目录: 项目根目录下的 output 文件夹
  - 构建类型: release

输出文件名格式:
  basefit-{版本号}[-{日期}].apk
  例如: basefit-1.0.0.apk 或 basefit-1.0.0-20260323.apk

跨平台兼容性:
  - Windows: cmd.exe, PowerShell, Git Bash, WSL
  - macOS: Terminal.app, iTerm2
  - Linux: gnome-terminal, konsole, xterm, etc.
""")
        sys.exit(0)

    main()
