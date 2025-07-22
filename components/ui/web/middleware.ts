import { NextResponse, NextRequest } from 'next/server';

export async function middleware(request: NextRequest) {
  // Check if user is authenticated, continue as normal
  const portalCookie = request.cookies.get('portalCookie');
  if (portalCookie) {
    NextResponse.next();
  }

  const url = request.nextUrl;

  // If the user is not authenticated, redirect to login page
  if (!portalCookie && url.pathname !== '/login' && !url.pathname.startsWith('/_next')) {
    const callbackURL = encodeURIComponent(`${url.pathname}${url.search}`);

    return NextResponse.redirect(
      new URL(
        `${process.env.NEXT_PUBLIC_BASE_PATH}/login?callbackURL=${callbackURL}`,
        request.url
      )
    );
  }

};