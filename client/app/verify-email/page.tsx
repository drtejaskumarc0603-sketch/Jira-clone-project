"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import axios from "@/lib/Axiosinstance";

export default function VerifyEmailPage() {
  const searchParams = useSearchParams();

  const [message, setMessage] = useState(
    "Verifying your email..."
  );

  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const token = searchParams.get("token");

    if (!token) {
      setMessage("Verification token is missing.");
      return;
    }

    const verify = async () => {
      try {
        await axios.get(
          `/api/users/verify-email?token=${encodeURIComponent(token)}`
        );

        setSuccess(true);
        setMessage(
          "Email verified successfully. You can now use your new email address."
        );
      } catch (err: any) {
        setMessage(
          err?.response?.data?.message ||
            err?.response?.data ||
            "Email verification failed."
        );
      }
    };

    verify();
  }, [searchParams]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-6">
      <div className="w-full max-w-md rounded-lg bg-white p-8 text-center shadow">
        <h1 className="mb-4 text-2xl font-bold">
          Email Verification
        </h1>

        <p
          className={
            success
              ? "text-green-600"
              : "text-gray-600"
          }
        >
          {message}
        </p>

        {success && (
          <a
            href="/"
            className="mt-6 inline-block rounded-md bg-blue-600 px-5 py-2 text-white"
          >
            Go to Login
          </a>
        )}
      </div>
    </div>
  );
}