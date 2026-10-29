import argparse
import os
import secrets
import sys
from pathlib import Path

# Add backend directory to sys.path
backend_dir = Path(__file__).resolve().parent.parent.parent
if str(backend_dir) not in sys.path:
    sys.path.insert(0, str(backend_dir))

from app.db.session import SessionLocal, engine, Base
from app.models.models import User, UserSettings
from app.core.security import get_password_hash


def create_initial_admin(
    email: str = "",
    password: str = "",
    force: bool = False
) -> dict:
    """
    Creates the initial administrator account securely.
    Credentials can be passed via arguments or environment variables:
    - ADMIN_EMAIL
    - ADMIN_PASSWORD
    If no password is provided, generates a secure random 24-char token.
    """
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    try:
        admin_email = (
            email
            or os.environ.get("ADMIN_EMAIL")
            or "admin@thoughtflow.ai"
        ).strip().lower()

        # Check if admin already exists
        existing_user = db.query(User).filter(User.email == admin_email).first()
        if existing_user and not force:
            print(f"[ThoughtFlow AI] Admin user already exists: {admin_email}")
            print("To reset password, re-run with --force flag.")
            return {"status": "exists", "email": admin_email}

        auto_generated = False
        admin_password = password or os.environ.get("ADMIN_PASSWORD")
        if not admin_password:
            admin_password = secrets.token_urlsafe(18)
            auto_generated = True

        hashed_pw = get_password_hash(admin_password)

        if existing_user:
            existing_user.password_hash = hashed_pw
            existing_user.role = "admin"
            db.commit()
            print(f"[ThoughtFlow AI] Updated password for existing admin: {admin_email}")
        else:
            new_admin = User(
                email=admin_email,
                password_hash=hashed_pw,
                role="admin"
            )
            db.add(new_admin)
            db.commit()
            db.refresh(new_admin)

            # Create default settings
            settings = UserSettings(
                user_id=new_admin.id,
                timezone="UTC",
                generation_time="07:00",
                publishing_time="09:00",
                selected_themes=["motivation", "discipline", "consistency", "resilience"],
                language="en",
                video_duration=15,
                narration_enabled=False,
                auto_publish_enabled=False,
                automation_enabled=False,
                daily_limit=5,
                visual_theme="gradient_purple",
                animation_style="smooth_zoom"
            )
            db.add(settings)
            db.commit()
            print(f"[ThoughtFlow AI] Initial administrator account created successfully!")

        print("=" * 60)
        print(f"  Admin Email    : {admin_email}")
        print(f"  Admin Password : {admin_password}")
        if auto_generated:
            print("  (Generated a secure random password. Please save it securely!)")
        print("=" * 60)

        return {
            "status": "created" if not existing_user else "updated",
            "email": admin_email,
            "password": admin_password
        }
    finally:
        db.close()


def main():
    parser = argparse.ArgumentParser(description="ThoughtFlow AI - Initial Admin Setup")
    parser.add_argument("--email", type=str, default="", help="Admin email address")
    parser.add_argument("--password", type=str, default="", help="Admin password (generated if omitted)")
    parser.add_argument("--force", action="store_true", help="Force update password if admin exists")
    args = parser.parse_args()

    create_initial_admin(
        email=args.email,
        password=args.password,
        force=args.force
    )


if __name__ == "__main__":
    main()
